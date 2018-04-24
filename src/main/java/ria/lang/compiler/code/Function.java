package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Binder;
import ria.lang.compiler.CType;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;
import ria.lang.compiler.code.BindRef;

import java.util.HashMap;
import java.util.Map;

public final class Function extends CapturingClosure implements Binder {
    private static final Code NEVER = new Code() {
        @Override
        public void gen(Context context) {
            throw new UnsupportedOperationException();
        }
    };

    String name; // name of the generated function class
    public Binder selfBind;
    public Code body;
    public String bindName; // function (self)binding name, if there is any
    Label restart; // used by tail-call optimizer
    Function outer; // outer function of directly-nested function
    // outer arguments to be saved in local registers (used for tail-call)
    CaptureRef argCaptures;
    // register used by argument (2 for merged inner function)
    int argVar = 1;
    // Marks function optimised as method and points to it's inner-most lambda
    Function methodImpl;
    // Function has been merged with its inner function.
    boolean merged;
    // Module has asked function to be a public (inner) class.
    // Useful for making Java code happy, if it wants to call the function.
    boolean publish;
    // methodImpl and only one live capture - carry it directly.
    boolean capture1;
    // function body has asked self reference (and the ref is not mutable)
    private CaptureRef selfRef;
    // argument value for inlined function
    private Code uncaptureArg;
    // How many times the argument has been used.
    // This counter is also used by argument nulling to determine
    // when it safe to assume that argument value is no more needed.
    private int argUsed;
    public final BindRef arg = new BindRef() {
        @Override
        public void gen(Context context) {
            if(uncaptureArg != null) {
                uncaptureArg.gen(context);
            } else {
                int t;
                context.load(argVar);
                // nulling...
                if(--argUsed == 0 && context.tainted == 0 &&
                    (t = type.deref().type) != RiaType.NUM &&
                    t != RiaType.BOOL) {
                    //context.insn(ACONST_NULL);
                    //context.varInsn(ASTORE, argVar);
                }
            }
        }

        @Override
        public boolean flagop(int fl) {
            return (fl & PURE) != 0;
        }
    };

    // Function is constant that can be statically shared.
    // Stores function instance in static final _ field and allows
    // direct-ref no-capture optimisations for function binding.
    private boolean shared;
    // Function uses local bindings from its module. Published function
    // should ensure module initialisation in this case, when called.
    private boolean moduleInit;
    // not in struct - capture final fields
    private boolean notInStruct;

    public Function(CType type) {
        this.type = type;
        arg.binder = this;
    }

    @Override
    public BindRef getRef(int line) {
        ++argUsed;
        return arg;
    }

    // uncaptures captured variables if possible
    // useful for function inlining, don't work with self-refs
    boolean uncapture(Code arg) {
        if(selfRef != null || merged) {
            return false;
        }
        for(Capture c = captures; c != null; c = c.next) {
            c.uncaptured = true;
        }
        uncaptureArg = arg;
        return true;
    }

    public void setBody(Code body) {
        this.body = body;
        if(body instanceof Function) {
            Function bodyFun = (Function)body;
            bodyFun.outer = this;
            if(argVar == 1 && !bodyFun.merged &&
                bodyFun.selfRef == null && captures == null) {
                merged = true;
                ++bodyFun.argVar;
            }
        }
    }

    @Override
    public BindRef refProxy(BindRef code) {
        if(code.flagop(DIRECT_BIND)) {
            if(code.flagop(MODULE_REQUIRED)) {
                moduleInit = true;
            }
            return code;
        }
        if(selfBind == code.binder && !code.flagop(ASSIGN)) {
            if(selfRef == null) {
                selfRef = new CaptureRef() {
                    @Override
                    public void gen(Context context) {
                        if(shared) {
                            Function.this.gen(context);
                        } else {
                            context.load(0).forceType("ria/lang/Fun");
                        }
                    }

                    @Override
                    public boolean flagop(int fl) {
                        // Don't be a capture when FunClass._ can be used
                        return (fl & DIRECT_BIND) != 0 && shared;
                    }
                };
                selfRef.binder = selfBind;
                selfRef.type = code.type;
                selfRef.ref = code;
                selfRef.origin = code.origin;

                selfRef.capturer = this;
            }
            return selfRef;
        }
        if(merged) {
            return code;
        }
        Capture c = captureRef(code);
        c.capturer = this;
        //expecting max 2 merged
        if(outer != null && outer.merged && (code == outer.selfRef || code == outer.arg)) {
            c.localVar = 1; // really evil hack for tail-recursion.
            c.uncaptured = true;
        }
        return c;
    }

    // called by mergeCaptures
    @Override
    void captureInit(Context fun, Capture c, int n) {
        if(methodImpl == null) {
            // c.getId() initialises the captures id as a side effect
            fun.cw.visitField(notInStruct ? ACC_PRIVATE | ACC_FINAL : 0,
                c.getId(fun), c.captureType(),
                null, null).visitEnd();
        } else if(capture1) {
            assert (n == 0);
            c.localVar = 0;
            fun.load(0).captureCast(c.captureType());
            fun.varInsn(ASTORE, 0);
        } else {
            c.localVar = -2 - n;
        }
    }

    private void prepareMethod(Context context) {
        // Map captures using binder as identity.
        Map<Binder, Capture> captureMapping = null;

        // This has to be done before mergeCaptures to have all binders.
        if(methodImpl != this &&
            (methodImpl != body || methodImpl.captures != null)) {
            captureMapping = new HashMap<>();

            // Function is binder for it's argument
            int argCounter = 0;
            for(Function f = this; f != methodImpl; f = (Function)f.body) {
                // just to hold localVar
                Capture tmp = new Capture();
                tmp.localVar = ++argCounter;
                f.argVar = argCounter; // merge messes up the pre-last capture
                captureMapping.put(f, tmp);
            }
            methodImpl.argVar = ++argCounter;

            for(Capture c = captures; c != null; c = c.next) {
                captureMapping.put(c.binder, c);
            }
            Capture tmp = new Capture();
            tmp.localVar = 0;
            captureMapping.put(selfBind, tmp);
        }

        // Create method
        if(bindName != null) {
            bindName = mangle(bindName);
        }
        bindName = context.methodName(bindName);
        StringBuilder sig = new StringBuilder(capture1 ? "(" : "([");
        for(int i = methodImpl.argVar + 2; --i >= 0; ) {
            if(i == 0) {
                sig.append(')');
            }
            sig.append("Ljava/lang/Object;");
        }
        Context m = context.newMethod(ACC_STATIC, bindName, sig.toString());

        // Removes duplicate captures and calls captureInit
        // (which sets captures localVar for our case).
        int captureCount = mergeCaptures(m, false);

        // Hijack the inner functions capture mapping...
        if(captureMapping != null) {
            for(Capture c = methodImpl.captures; c != null; c = c.next) {
                Object mapped = captureMapping.get(c.binder);
                if(mapped != null) {
                    c.localVar = ((Capture)mapped).localVar;
                    c.ignoreGet = c.localVar > 0;
                } else { // Capture was stolen by direct bind?
                    Capture x = c;
                    while(x.capturer != this && x.ref instanceof Capture) {
                        x = (Capture)x.ref;
                    }
                    if(x.uncaptured) {
                        c.ref = x.ref;
                        c.uncaptured = true;
                    }
                }
            }
        }

        // Generate method body
        name = context.className;
        m.localVarCount = methodImpl.argVar + 1; // capturearray, args
        methodImpl.genClosureInit(m);
        m.visitLabel(methodImpl.restart = new Label());
        methodImpl.body.gen(m);
        methodImpl.restart = null;
        m.insn(ARETURN);
        m.closeMethod();

        if(!shared && !capture1) {
            context.intConst(captureCount);
            context.typeInsn(ANEWARRAY, "java/lang/Object");
        }
    }

    /*
     * For functions, this generates the function class.
     * An instance is also given, but capture fields are not initialised
     * (the captures are set later in the finishGen).
     */
    boolean prepareGen(Context context, boolean notStruct) {
        if(methodImpl != null) {
            prepareMethod(context);
            return false;
        }

        if(merged) { // 2 nested lambdas have been optimised into 1
            Function inner = (Function)body;
            inner.bindName = bindName;
            boolean res = inner.prepareGen(context, notStruct);
            name = inner.name;
            return res;
        }

        notInStruct = notStruct;
        if(bindName == null) {
            bindName = "";
        }
        name = context.compilation.createClassName(context,
            context.className, mangle(bindName));

        publish &= shared;
        String funClass =
            argVar == 2 ? "ria/lang/Fun2" : "ria/lang/Fun";
        Context fun = context.newClass(ACC_SUPER | ACC_FINAL, name, funClass, null, 0);

        if(publish) {
            fun.markInnerClass(context, ACC_STATIC | ACC_FINAL);
        }
        mergeCaptures(fun, false);
        if(!notStruct) {
            fun.createInit(shared ? ACC_PRIVATE : 0, funClass);
        }

        Context apply = argVar == 2
            ? fun.newMethod(ACC_PUBLIC + ACC_FINAL, "apply",
            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
            : fun.newMethod(ACC_PUBLIC + ACC_FINAL, "apply",
            "(Ljava/lang/Object;)Ljava/lang/Object;");
        apply.localVarCount = argVar + 1; // this, arg

        if(argCaptures != null) {
            // Tail recursion needs all args to be in local registers
            // - otherwise it couldn't modify them safely before restarting
            Capture[] args = argCaptures.argCaptures();
            for(Capture c : args) {
                if(c != null && !c.uncaptured) {
                    c.gen(apply);
                    c.localVar = apply.localVarCount;
                    c.ignoreGet = true;
                    apply.varInsn(ASTORE, apply.localVarCount++);
                }
            }
        }
        if(moduleInit && publish) {
            apply.methodInsn(INVOKESTATIC, context.className, "init", "()V");
        }
        genClosureInit(apply);
        apply.visitLabel(restart = new Label());
        body.gen(apply);
        restart = null;
        apply.insn(ARETURN);
        apply.closeMethod();

        Context valueContext =
            shared ? fun.newMethod(ACC_STATIC, "<clinit>", "()V") : context;
        valueContext.typeInsn(NEW, name);
        valueContext.insn(DUP);
        if(notStruct) { // final fields must be initialized in constructor
            StringBuilder sigb = new StringBuilder("(");
            for(Capture c = captures; c != null; c = c.next) {
                if(!c.uncaptured) {
                    sigb.append(c.captureType());
                }
            }
            String sig = sigb.append(")V").toString();
            Context init = fun.newMethod(shared ? ACC_PRIVATE : 0, "<init>", sig);
            init.load(0).methodInsn(INVOKESPECIAL, funClass, "<init>", "()V");
            int counter = 0;
            for(Capture c = captures; c != null; c = c.next) {
                if(!c.uncaptured) {
                    c.captureGen(valueContext);
                    init.load(0).load(++counter)
                        .fieldInsn(PUTFIELD, name, c.id, c.captureType());
                }
            }
            init.insn(RETURN);
            init.closeMethod();
            valueContext.visitInit(name, sig);
            valueContext.forceType("ria/lang/Fun");
        } else {
            valueContext.visitInit(name, "()V");
        }
        if(shared) {
            fun.cw.visitField(ACC_STATIC | ACC_FINAL,
                "_", "Lria/lang/Fun;", null, null).visitEnd();
            valueContext.fieldInsn(PUTSTATIC, name, "_", "Lria/lang/Fun;");
            valueContext.insn(RETURN);
            valueContext.closeMethod();
        }
        return notStruct;
    }

    void finishGen(Context context) {
        if(merged) {
            ((Function)body).finishGen(context);
            return;
        }
        boolean meth = methodImpl != null;
        int counter = -1;
        // Capture a closure
        for(Capture c = captures; c != null; c = c.next) {
            if(c.uncaptured) {
                continue;
            }
            if(capture1) {
                c.captureGen(context);
                return;
            }
            context.insn(DUP);
            if(meth) {
                context.intConst(++counter);
                c.captureGen(context);
                context.insn(AASTORE);
            } else {
                c.captureGen(context);
                context.fieldInsn(PUTFIELD, name, c.id, c.captureType());
            }
        }
        context.forceType(meth ? "[Ljava/lang/Object;" : "ria/lang/Fun");
    }

    @Override
    public boolean flagop(int fl) {
        return merged ? body.flagop(fl) :
            (fl & (PURE | CONST)) != 0 && (shared || captures == null);
    }

    // Check whether all captures are actually static constants.
    // If so, the function value should also be optimised into shared constant.
    @Override
    boolean prepareConst(Context context) {
        if(shared) // already optimised into static constant value
        {
            return true;
        }

        BindExpr bindExpr = null;
        // First try determine if we can reduce into method.
        if(selfBind instanceof BindExpr &&
            (bindExpr = (BindExpr)selfBind).evalId == -1 &&
            bindExpr.result != null) {
            int arityLimit = 99999999;
            for(BindExpr.Ref i = bindExpr.refs; i != null; i = i.next) {
                if(arityLimit > i.arity) {
                    arityLimit = i.arity;
                }
            }
            int arity = 0;
            Function impl = this;
            while(++arity < arityLimit && impl.body instanceof Function) {
                impl = (Function)impl.body;
            }

            if(arity > 0 && arityLimit > 0 && (arity > 1 || !merged)) {
                if(merged) { // steal captures and unmerge
                    captures = ((Function)body).captures;
                    merged = false;
                }
                methodImpl = impl.merged ? impl.outer : impl;
                bindExpr.setCaptureType("[Ljava/lang/Object;");
            }
        }

        if(merged) {
            // merged functions are hollow, their juice is in the inner function
            Function inner = (Function)body;
            inner.bindName = bindName;
            inner.publish = publish;
            if(inner.prepareConst(context)) {
                name = inner.name; // used by gen
                return true;
            }
            return false;
        }

        // this can be optimised into "const x", so don't touch.
        if(argUsed == 0 && argVar == 1 &&
            methodImpl == null && body.flagop(PURE)) {
            return false; //captures == null;
        }

        // Uncapture the direct bindings.
        Capture prev = null;
        int liveCaptures = 0;
        for(Capture c = captures; c != null; c = c.next) {
            if(c.ref.flagop(DIRECT_BIND)) {
                c.uncaptured = true;
                if(prev == null) {
                    captures = c.next;
                } else {
                    prev.next = c.next;
                }
            } else {
                // Why are existing uncaptured ones preserved?
                // Does some checks (selfref, args??) after prepareConst?
                if(!c.uncaptured) {
                    ++liveCaptures;
                }
                prev = c;
            }
        }

        if(methodImpl != null && liveCaptures == 1) {
            capture1 = true;
            bindExpr.setCaptureType("java/lang/Object");
        }

        // If all captures were uncaptured, then the function can
        // (and will) be optimised into shared static constant.
        if(liveCaptures == 0) {
            shared = true;
            prepareGen(context, false);
        }
        return liveCaptures == 0;
    }

    @Override
    public void gen(Context context) {
        if(shared) {
            if(methodImpl == null) {
                context.fieldInsn(GETSTATIC, name, "_", "Lria/lang/Fun;");
            } else {
                context.insn(ACONST_NULL);
            }
        } else if(!merged && argUsed == 0 && body.flagop(PURE) && uncapture(NEVER)) {
            // This lambda can be optimised into "const x", so do it.
            genClosureInit(context);
            context.typeInsn(NEW, "ria/lang/Const");
            context.insn(DUP);
            body.gen(context);
            context.visitInit("ria/lang/Const", "(Ljava/lang/Object;)V");
            context.forceType("ria/lang/Fun");
        } else if(prepareConst(context)) {
            if(methodImpl == null) {
                context.fieldInsn(GETSTATIC, name, "_", "Lria/lang/Fun;");
            } else {
                context.insn(ACONST_NULL);
            }
        } else if(!prepareGen(context, true)) {
            finishGen(context);
        }
    }
}
