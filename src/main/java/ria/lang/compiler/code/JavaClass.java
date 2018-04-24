package ria.lang.compiler.code;

import ria.lang.compiler.Binder;
import ria.lang.compiler.CType;
import ria.lang.compiler.CaptureWrapper;
import ria.lang.compiler.CodeGen;
import ria.lang.compiler.CompileException;
import ria.lang.compiler.Context;
import ria.lang.compiler.JavaClassNotFoundException;
import ria.lang.compiler.JavaType;
import ria.lang.compiler.JavaTypeReader;
import ria.lang.compiler.RiaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JavaClass extends CapturingClosure implements Runnable {
    private String className;
    private String[] implement;
    private RiaType.ClassBinding parentClass;
    private List<Field> fields = new ArrayList<>();
    private List<Meth> methods = new ArrayList<>();
    private Field serialVersion;
    private JavaExpr superInit;
    private final boolean isPublic;
    private boolean hasStatic;
    private int captureCount;
    private Map<String, Object[]> accessors;
    private Context classContext;
    private Capture merged;
    private final int cline; // for duplicate class error
    public CType classType;
    public final Meth constr = new Meth();
    public final Binder self;
    public Binder superRef;

    static class Arg extends BindRef implements Binder {
        int argn;
        final CType javaType;
        private boolean isSuper;

        Arg(CType type, boolean isSuper) {
            this.javaType = type;
            this.type = JavaType.convertValueType(type);
            this.isSuper = isSuper;
            binder = this;
        }

        @Override
        public BindRef getRef(int line) {
            if (isSuper && line >= 0) {
                throw new CompileException(line, 0, "super cannot be used as a value");
            }
            return this;
        }

        @Override
        public void gen(Context context) {
            context.load(argn);
            if (javaType.type == RiaType.JAVA_ARRAY) {
                context.forceType(JavaType.descriptionOf(javaType));
            } else if (javaType.javaType.description.charAt(0) == 'L') {
                context.forceType(javaType.javaType.className());
            }
        }

        @Override
        public boolean flagop(int flag) {
            return (flag & DIRECT_THIS) != 0 && argn == 0;
        }
    }

    public static class Meth extends JavaType.Method implements Closure {
        private List<Arg> args = new ArrayList<>();
        private AbstractClosure closure = new LoopExpr(); // just for closure init
        private int line;
        Capture captures;
        public Code code;

        public Binder addArg(CType type) {
            Arg arg = new Arg(type, false);
            args.add(arg);
            arg.argn = (access & ACC_STATIC) == 0
                            ? args.size() : args.size() - 1;
            return arg;
        }

        @Override
        public BindRef refProxy(BindRef code) {
            return code; // method don't capture - this is outer classes job
        }

        @Override
        public void addVar(BindExpr binder) {
            closure.addVar(binder);
        }
        
        void init() {
            arguments = new CType[args.size()];
            for (int i = 0; i < arguments.length; ++i) {
                Arg arg = args.get(i);
                arguments[i] = arg.javaType;
            }
            sig = name.concat(super.descr(null));
            descr = null;
        }

        @Override
        public String descr(String extra) {
            if (descr != null) {
                return descr;
            }
            StringBuilder additionalArgs = new StringBuilder();
            for (Capture c = captures; c != null; c = c.next) {
                additionalArgs.append(c.captureType());
            }
            return super.descr(additionalArgs.toString());
        }

        void convertArgs(Context context) {
            int n = (access & ACC_STATIC) == 0 ? 1 : 0;
            int at = n;
            for (int i = 0; i < arguments.length; ++i) {
                ++at;
                if(arguments[i].type == RiaType.JAVA) {
                    String descr = arguments[i].javaType.description;
                    if(Objects.equals(descr, "Ljava/lang/String;") || descr.charAt(0) != 'L') {
                        --at;
                        at += loadArg(context, arguments[i], at);
                        JavaExpr.convertValue(context, arguments[i]);
                        context.varInsn(ASTORE, i + n);
                    }
                }
            }
            context.localVarCount = at;
        }

        void gen(Context context) {
            context = context.newMethod(access, name, descr(null));
            if ((access & ACC_ABSTRACT) != 0) {
                context.closeMethod();
                return;
            }
            convertArgs(context);
            closure.genClosureInit(context);
            JavaExpr.convertedArg(context, code, returnType, line);
            if (returnType.type == RiaType.UNIT) {
                context.insn(POP);
                context.insn(RETURN);
            } else {
                genRet(context, returnType);
            }
            context.closeMethod();
        }
    }

    final class Field extends Code implements Binder, CaptureWrapper, CodeGen {
        private String name; // mangled name
        private String javaType;
        private String descr;
        Code value;
        private final boolean var;
        private int access = ACC_PRIVATE;
        private boolean directConst;

        Field(String name, Code value, boolean var) {
            this.name = name;
            this.value = value;
            this.var = var;
        }

        @Override
        public void genPreGet(Context context) {
            if (!directConst) {
                context.load(0);
            }
        }

        @Override
        public void genGet(Context context) {
            if (directConst) {
                value.gen(context);
            } else {
                context.fieldInsn(GETFIELD, className, name, descr);
            }
        }

        @Override
        public void genSet(Context context, Code value) {
            value.gen(context);
            context.typeInsn(CHECKCAST, javaType);
            context.fieldInsn(PUTFIELD, className, name, descr);
        }

        @Override
        public Object captureIdentity() {
            return JavaClass.this;
        }

        @Override
        public String captureType() {
            return classType.javaType.description;
        }

        @Override
        public void gen2(Context context, Code value, int __) {
            genPreGet(context);
            genSet(context, value);
            context.insn(ACONST_NULL);
        }

        @Override
        public BindRef getRef(int line) {
            if (javaType == null) {
                if (Objects.equals(name, "_")) {
                    throw new IllegalStateException("NO _ REF");
                }
                javaType = Code.javaType(value.type);
                descr = 'L' + javaType + ';';
            }
            BindRef ref = new BindRef() {
                @Override
                public void gen(Context ctx) {
                    genPreGet(ctx);
                    genGet(ctx);
                }

                @Override
                public Code assign(final Code value) {
                    return var ?
                            new SimpleCode(Field.this, value, null, 0) : null;
                }

                @Override
                public boolean flagop(int fl) {
                    return (fl & ASSIGN) != 0 && var ||
                           (fl & (CONST | DIRECT_BIND)) != 0 && directConst ||
                           (fl & PURE) != 0 && !var;
                }

                @Override
                public CaptureWrapper capture() {
                    if (!var) {
                        return null;
                    }
                    access = ACC_SYNTHETIC; // clear private
                    return Field.this;
                }
            };
            ref.type = value.type;
            ref.binder = this;
            return ref;
        }

        @Override
        public void gen(Context context) {
            if (this == serialVersion) {
                // hack to allow defining serialVersionUID
                Long v = (((NumericConstant)value).num).longValue();
                context.cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                                  name, "J", null, v);
                directConst = true;
            } else if (javaType == null) {
                // _ = or just unused binding
                value.gen(context);
                context.insn(POP);
            } else if (!var && value.prepareConst(context)) {
                directConst = true;
            } else {
                context.cw.visitField(var ? access : access | ACC_FINAL,
                                  name, descr, null, null).visitEnd();
                genPreGet(context);
                genSet(context, value);
            }
        }
    }

    public JavaClass(String className, boolean isPublic, int line) {
        type = RiaType.UNIT_TYPE;
        this.className = className;
        classType = new CType(RiaType.JAVA, RiaType.NO_PARAM);
        classType.javaType = JavaType.createNewClass(className, this);
        self = new Arg(classType, false);
        constr.name = "<init>";
        constr.returnType = RiaType.UNIT_TYPE;
        constr.className = className;
        constr.access = isPublic ? ACC_PUBLIC : 0;
        this.isPublic = isPublic;
        cline = line;
    }

    private static int loadArg(Context context, CType argType, int n) {
        int ins = ALOAD;
        if (argType.type == RiaType.JAVA) {
            switch (argType.javaType.description.charAt(0)) {
                case 'D': ins = DLOAD; break;
                case 'F': ins = FLOAD; break;
                case 'J': ins = LLOAD; break;
                case 'L': break;
                default : ins = ILOAD;
            }
        }
        context.varInsn(ins, n);
        return ins == DLOAD ? 2 : 1;
    }

    static void genRet(Context context, CType returnType) {
        int ins = ARETURN;
        if (returnType.type == RiaType.JAVA) {
            switch (returnType.javaType.description.charAt(0)) {
                case 'D': ins = DRETURN; break;
                case 'F': ins = FRETURN; break;
                case 'J': ins = LRETURN; break;
                case 'L': break;
                case 'V': ins = RETURN; break;
                default : ins = IRETURN;
            }
        }
        context.insn(ins);
    }

    public void init(RiaType.ClassBinding parentClass, String[] interfaces) {
        implement = interfaces;
        this.parentClass = parentClass;
        CType t = new CType(RiaType.JAVA, RiaType.NO_PARAM);
        t.javaType = parentClass.type.javaType.dup();
        t.javaType.implementation = this;
        t.javaType.publicMask = ACC_PUBLIC | ACC_PROTECTED;
        superRef = new Arg(t, true);
    }

    public Meth addMethod(String name, CType returnType,
                          String mod, int line) {
        Meth m = new Meth();
        m.name = name;
        m.returnType = returnType;
        m.className = className;
        m.access = Objects.equals(mod, "static-method") ? ACC_PUBLIC + ACC_STATIC
                 : Objects.equals(mod, "abstract-method") ? ACC_PUBLIC + ACC_ABSTRACT
                 : ACC_PUBLIC;
        if ((m.access & ACC_STATIC) != 0) {
            hasStatic = true;
        }
        m.line = line;
        methods.add(m);
        return m;
    }

    public Binder addField(Code value, boolean var, String name) {
        Field field;
        if (Objects.equals(name, "serialVersionUID") && !var &&
                serialVersion == null && value instanceof NumericConstant) {
            serialVersion = field = new Field(name, value, false);
        } else {
            field = new Field("$" + fields.size(), value, var);
        }
        fields.add(field);
        return field;
    }

    @Override
    public BindRef refProxy(BindRef code) {
        if (code.flagop(DIRECT_BIND)) {
            return code;
        }
        if (!isPublic) {
            return captureRef(code);
        }
        code.forceDirect();
        return code;
    }

    public void superInit(JavaType.Method init, Code[] args, int line) {
        superInit = new JavaExpr(null, init, args, line);
    }

    public void close() throws JavaClassNotFoundException {
        constr.init();
        JavaTypeReader t = new JavaTypeReader();
        t.constructors.add(constr);
        for(Object method : methods) {
            Meth m = (Meth)method;
            m.init();
            ((m.access & ACC_STATIC) != 0 ? t.staticMethods : t.methods).add(m);
        }
        t.parent = parentClass.type.javaType;
        t.className = className;
        t.interfaces = implement;
        t.access = isPublic ? ACC_PUBLIC : 0;
        classType.javaType.publicMask = ACC_PUBLIC | ACC_PROTECTED;
        classType.javaType.resolve(t);
    }

    // must be called after close
    public BindRef[] getCaptures() {
        captureCount = mergeCaptures(null, true);
        BindRef[] r = new BindRef[captureCount];
        int n = 0;
        for (Capture c = captures; c != null; c = c.next) {
            r[n++] = c.ref;
        }
        return r;
    }

    // called by mergeCaptures
    @Override
    void captureInit(Context fun, Capture c, int n) {
        c.id = "_" + n;
        // for super arguments
        c.localVar = n + constr.args.size() + 1;
    }

    @Override
    void onMerge(Capture removed) {
        removed.next = merged;
        merged = removed;
    }

    String getAccessor(JavaType.Method method, String descr,
                       boolean invokeSuper) {
        if (accessors == null) {
            accessors = new HashMap<>();
        }
        String sig = method.sig;
        if (invokeSuper) {
            sig = "*".concat(method.sig);
        }
        Object[] accessor = accessors.get(sig);
        if (accessor == null) {
            accessor = new Object[] { "access$" + accessors.size(), method,
                                      descr, invokeSuper ? "" : null };
            accessors.put(method.sig, accessor);
        }
        return (String) accessor[0];
    }

    String getAccessor(JavaType.Field field, String descr, boolean write) {
        String key = (write ? "{" : "}").concat(field.name);
        if (accessors == null) {
            accessors = new HashMap<>();
        }
        Object[] accessor = accessors.computeIfAbsent(key, k -> new Object[] {"access$" + accessors.size(), field,
            descr, write ? "" : null, null});
        return (String) accessor[0];
    }

    @Override
    public void gen(Context context) {
        int i, cnt;
        Capture c;

        constr.captures = captures;
        context.insn(ACONST_NULL);
        Context clc = context.newClass(classType.javaType.access | ACC_SUPER,
                        className, parentClass.type.javaType.className(),
                        implement, cline);
        clc.fieldCounter = captureCount;
        // block using our method names ;)
        for (i = 0, cnt = methods.size(); i < cnt; ++i) {
            clc.usedMethodNames.put((methods.get(i)).name, null);
        }
        if (!isPublic) {
            clc.markInnerClass(context.constants.context, ACC_STATIC);
        }
        Context init = clc.newMethod(constr.access, "<init>", constr.descr(null));
        if (isPublic && !hasStatic) {
            init.methodInsn(INVOKESTATIC, context.className, "init", "()V");
        }
        constr.convertArgs(init);
        genClosureInit(init);
        superInit.genCall(init.load(0), parentClass.getCaptures(),
                          INVOKESPECIAL);
        // extra arguments are used for smuggling in captured bindings
        int n = constr.arguments.length;
        for (c = captures; c != null; c = c.next) {
            c.localVar = -1; // reset to using this
            clc.cw.visitField(ACC_FINAL | ACC_PRIVATE, c.id, c.captureType(),
                              null, null).visitEnd();
            init.load(0).load(++n)
                .fieldInsn(PUTFIELD, className, c.id, c.captureType());
        }
        for (c = merged; c != null; c = c.next) {
            c.localVar = -1; // reset all merged captures also
        }
        for (i = 0, cnt = fields.size(); i < cnt; ++i) {
            (fields.get(i)).gen(init);
        }
        init.insn(RETURN);
        init.closeMethod();
        for (i = 0, cnt = methods.size(); i < cnt; ++i) {
            (methods.get(i)).gen(clc);
        }
        if (isPublic && hasStatic) {
            Context clinit = clc.newMethod(ACC_STATIC, "<clinit>", "()V");
            clinit.methodInsn(INVOKESTATIC, context.className, "init", "()V");
            clinit.insn(RETURN);
            clinit.closeMethod();
        }
        classContext = clc;
        context.compilation.postGen.add(this);
    }

    // postGen hook. accessors can be added later than the class gen is called
    @Override
    public void run() {
        if (accessors == null) {
            return;
        }
        for(Object o : accessors.values()) {
            Object[] accessor = (Object[])o;
            int acc = ACC_STATIC;
            JavaType.Method m = null;
            if(accessor.length == 4) { // method
                m = (JavaType.Method)accessor[1];
                acc = m.access & ACC_STATIC;
            }
            Context mc = classContext.newMethod(acc | ACC_SYNTHETIC,
                (String)accessor[0],
                (String)accessor[2]);
            if(m != null) { // method
                int start = 0;
                int insn = INVOKESTATIC;
                if((acc & ACC_STATIC) == 0) {
                    insn = accessor[3] == null ? INVOKEVIRTUAL : INVOKESPECIAL;
                    start = 1;
                    mc.load(0);
                }
                for(int j = 0; j < m.arguments.length; ) {
                    j += loadArg(mc, m.arguments[j], j + start);
                }
                mc.methodInsn(insn, accessor[3] == null ? className :
                        parentClass.type.javaType.className(),
                    m.name, m.descr(null));
                genRet(mc, m.returnType);
            } else { // field
                JavaType.Field f = (JavaType.Field)accessor[1];
                int insn = GETSTATIC, reg = 0;
                if((f.access & ACC_STATIC) == 0) {
                    mc.load(reg++);
                    insn = GETFIELD;
                }
                if(accessor[3] != null) {
                    mc.load(reg);
                    insn = insn == GETFIELD ? PUTFIELD : PUTSTATIC;
                }
                mc.fieldInsn(insn, className, f.name,
                    JavaType.descriptionOf(f.type));
                if(accessor[3] != null) {
                    mc.insn(RETURN);
                } else {
                    genRet(mc, f.type);
                }
            }
            mc.closeMethod();
        }
    }
}
