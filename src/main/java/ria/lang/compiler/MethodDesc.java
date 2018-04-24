package ria.lang.compiler;

import org.objectweb.asm.Opcodes;
import ria.lang.compiler.code.BindRef;
import ria.lang.compiler.code.Closure;
import ria.lang.compiler.code.Code;
import ria.lang.compiler.code.Function;
import ria.lang.compiler.code.JavaClass;
import ria.lang.compiler.nodes.Bind;
import ria.lang.compiler.nodes.Node;
import ria.lang.compiler.nodes.XNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class MethodDesc extends RiaType {
    Binder[] arguments;
    String[] names;
    JavaClass.Meth method;
    Node[] m;
    boolean isStatic;

    MethodDesc(JavaClass.Meth method, Node argList, Scope scope) {
        this.method = method;
        Node[] args = ((XNode)argList).expr;
        arguments = new Binder[args.length / 2];
        names = new String[arguments.length];
        for(int i = 0, j = 0; i < arguments.length; ++i, j += 2) {
            String name = args[j + 1].sym();
            for(int k = 0; k < i; ++k) {
                if(Objects.equals(name, names[k])) {
                    throw new CompileException(args[j + 1], "Duplicate argument name (" + name + ")");
                }
            }
            names[i] = name;
            arguments[i] =
                method.addArg(JavaType.typeOfName(args[j].sym(), scope));
        }
    }

    /*
     * (class Foo (argument-list int x) (extends Object (arguments))
     * (foo = x)
     * (method String getBlaah (argument-list int y) (concat (x + y))))
     */
    static JavaClass defineClass(XNode cl, boolean topLevel,
                                 Scope[] scope_, int depth) {
        Scope scope = new Scope(scope_[0], null, null);
        String className = cl.expr[0].sym();
        String packageName = scope.ctx.packageName;
        Compiler cctx = scope.ctx.compiler;
        if(!topLevel) {
            className = cctx.createClassName(null, scope.ctx.className, className);
        } else if(packageName != null && packageName.length() != 0) {
            className = packageName + '/' + className;
        }
        cctx.addClass(className, null, cl.line);
        JavaClass c = new JavaClass(className, topLevel, cl.line);
        scope.closure = c; // to proxy super-class closures

        ClassBinding parentClass = null;
        Node[] extend = ((XNode)cl.expr[2]).expr;
        Node[] superArgs = null;
        Node superNode = cl;

        // collect parent class / interfaces (extends)
        List<String> interfaces = new ArrayList<>();
        for(int i = 0; i < extend.length; i += 2) {
            ClassBinding cb =
                resolveFullClass(extend[i].sym(), scope, true, extend[i]);
            JavaType jt = cb.type.javaType.resolve(extend[i]);
            jt.checkPackage(extend[i], packageName);
            Node[] args = ((XNode)extend[i + 1]).expr;
            if(jt.isInterface()) {
                if(args != null) {
                    throw new CompileException(extend[i + 1], "Cannot give arguments to interface");
                }
                interfaces.add(jt.className());
            } else if(parentClass != null) {
                throw new CompileException(extend[i],
                    "Cannot extend multiple non-interface classes (" +
                        parentClass.type.javaType.dottedName() +
                        " and " + jt.dottedName() + ')');
            } else {
                parentClass = cb;
                superArgs = args;
                superNode = extend[i];
            }
        }
        if(parentClass == null) {
            parentClass = new ClassBinding(OBJECT_TYPE);
        }
        parentClass.type.javaType.resolve(cl);
        c.init(parentClass, interfaces.toArray(new String[0]));
        scope = new Scope(scope_[0], cl.expr[0].sym(), null);
        LocalClassBinding binding = new LocalClassBinding(c.classType);
        scope.importClass = binding;
        scope_[0] = scope;
        MethodDesc consDesc = new MethodDesc(c.constr, cl.expr[1], scope);

        // method definitions
        List<MethodDesc> methods = new ArrayList<>();
        for(int i = 3; i < cl.expr.length; ++i) {
            String kind = cl.expr[i].kind;
            if(!Objects.equals(kind, "method") && !Objects.equals(kind, "static-method")
                && !Objects.equals(kind, "abstract-method")) {
                continue;
            }
            Node[] m = ((XNode)cl.expr[i]).expr;
            CType returnType = Objects.equals(m[0].sym(), "void") ? UNIT_TYPE :
                JavaType.typeOfName(m[0].sym(), scope);
            JavaClass.Meth meth =
                c.addMethod(m[1].sym(), returnType, kind,
                    m.length > 3 ? m[3].line : 0);
            MethodDesc md = new MethodDesc(meth, m[2], scope);
            if(Objects.equals(kind, "abstract-method")) {
                continue;
            }
            md.m = m;
            if((md.isStatic = !Objects.equals(kind, "method")) && !topLevel) {
                throw new CompileException(cl.expr[i], "Static methods are " +
                    "allowed only in classes defined in the module top-level");
            }
            methods.add(md);
        }

        try {
            c.close();
        } catch(JavaClassNotFoundException ex) {
            throw new CompileException(cl, ex);
        }
        consDesc.check(cl.expr[1], scope.ctx.packageName);
        for(Object method2 : methods) {
            MethodDesc md = (MethodDesc)method2;
            md.check(md.m[2], scope.ctx.packageName);
        }
        c.classType.javaType.checkAbstract();

        // constructor arguments
        Scope staticScope = new Scope(scope, null, null);
        staticScope.closure = c;
        Scope[] localRef = {staticScope};
        Scope consScope = consDesc.bindScope(staticScope, c, localRef);
        Scope local = localRef[0];

        if(superArgs == null) {
            superArgs = new Node[0];
        }
        Code[] initArgs = RiaAnalyzer.mapArgs(0, superArgs, consScope, depth);
        JavaType.Method superCons =
            JavaType.resolveConstructor(superNode, parentClass.type,
                initArgs, false)
                .check(superNode, packageName, Opcodes.ACC_PROTECTED);
        c.superInit(superCons, initArgs, superNode.line);

        local = new Scope(local, "super", c.superRef);

        // field defs
        for(int i = 3; i < cl.expr.length; ++i) {
            if(cl.expr[i] instanceof Bind) {
                Bind bind = (Bind)cl.expr[i];
                if(bind.property) {
                    throw new CompileException(bind,
                        "Class field cannot be a property");
                }
                Binder binder;
                Code code;
                if(Objects.equals(bind.expr.kind, "lambda") && !Objects.equals(bind.name, "_")) {
                    Function lambda = new Function(new CType(depth + 1));
                    lambda.selfBind = binder =
                        c.addField(lambda, bind.mutable, null);
                    // binding this for lambdas is unsafe, but useful
                    Scope funScope = new Scope(local, "this", c.self);
                    if(!bind.unbind) {
                        funScope = new Scope(funScope, bind.name, binder);
                    }
                    RiaAnalyzer.lambdaBind(lambda, bind, funScope, depth + 1);
                    code = lambda;
                } else {
                    code = RiaAnalyzer.analyze(bind.expr, local, depth + 1);
                    binder = c.addField(code, bind.mutable, bind.name);
                    if(bind.type != null) {
                        RiaAnalyzer.isOp(bind, bind.type, code, scope, depth);
                    }
                }
                if(!Objects.equals(bind.name, "_")) {
                    local = bind(bind.name, code.type, binder,
                        bind.mutable ? RESTRICT_ALL : code.polymorph
                            ? RESTRICT_POLY : 0, depth, local);
                }
            }
        }

        local = new Scope(local, "this", c.self);

        // analyze method bodies
        for(Object method1 : methods) {
            MethodDesc md = (MethodDesc)method1;
            md.init(md.isStatic ? staticScope : local, depth);
        }

        binding.init(c.getCaptures());
        return c;
    }

    Scope bindScope(Scope scope, JavaClass regField, Scope fields[]) {
        for(int i = 0; i < arguments.length; ++i) {
            scope = new Scope(scope, names[i], arguments[i]);
            if(regField != null) {
                Binder field =
                    regField.addField(arguments[i].getRef(0), false, null);
                fields[0] = new Scope(fields[0], names[i], field);
            }
        }
        return scope;
    }

    private void check(CType t, Node node, String packageName) {
        while(t.type == RiaType.JAVA_ARRAY) {
            t = t.param[0];
        }
        if(t.type == RiaType.JAVA && t.javaType.description.charAt(0) == 'L') {
            t.javaType.resolve(node).checkPackage(node, packageName);
        }
    }

    void check(Node argList, String packageName) {
        Node[] args = ((XNode)argList).expr;
        for(int i = 0; i < method.arguments.length; ++i) {
            check(method.arguments[i], args[i], packageName);
        }
        if(m != null) // constructors don't have m
        {
            check(method.returnType, m[0], packageName);
        }
    }

    void init(Scope mscope, int depth) {
        Scope bodyScope = bindScope(mscope, null, null);
        if(bodyScope == mscope) {
            bodyScope = new Scope(bodyScope, null, null);
        }
        bodyScope.closure = method; // for bind mutable collection
        method.code = RiaAnalyzer.analyze(m[3], bodyScope, depth);
        if(JavaType.isAssignable(m[3], method.returnType,
            method.code.type, true) < 0) {
            try {
                unify(method.code.type, method.returnType);
            } catch(TypeException ex) {
                throw new CompileException(m[3], "Cannot return " + method.code.type + " as " + method.returnType);
            }
        }
    }

    static final class LocalClassBinding extends ClassBinding {
        private List<Closure> proxies;
        private LocalClassBinding next;
        private BindRef[] captures;

        LocalClassBinding(CType classType) {
            super(classType);
        }

        @Override
        public BindRef[] getCaptures() {
            if(captures == null) {
                throw new IllegalStateException("Captures not initialized");
            }
            return captures;
        }

        @Override
        ClassBinding dup(List<Closure> proxies) {
            LocalClassBinding r = new LocalClassBinding(type);
            r.proxies = proxies;
            if(captures != null) {
                r.proxy(captures);
            } else {
                r.next = next;
                next = r;
            }
            return r;
        }

        private void proxy(BindRef[] captures) {
            if(captures.length == 0 || proxies.size() == 0) {
                this.captures = captures;
                return;
            }
            BindRef[] ca = new BindRef[captures.length];
            System.arraycopy(captures, 0, ca, 0, captures.length);
            // proxies were collected in reverse order (inner-first)
            for(int i = proxies.size(); --i >= 0; ) {
                Closure c = proxies.get(i);
                for(int j = 0; j < ca.length; ++j) {
                    ca[j] = c.refProxy(ca[j]);
                }
            }
            this.captures = ca;
            proxies = null;
        }

        void init(BindRef[] captures) {
            LocalClassBinding cb = this;
            while((cb = cb.next) != null) {
                cb.proxy(captures);
            }
            this.captures = captures;
        }
    }
}
