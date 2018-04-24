package ria.lang.compiler.code;

import ria.lang.compiler.CType;
import ria.lang.compiler.Context;

public class Apply extends Code {
    final Code fun, arg;
    final int line;
    int arity = 1;
    BindExpr.Ref ref;

    Apply(CType res, Code fun, Code arg, int line) {
        type = res;
        this.fun = fun;
        this.arg = arg;
        this.line = line;
    }

    @Override
    public void gen(Context context) {
        Function f;
        int argc = 0;

        // Function sets its methodImpl field, if it has determined that
        // it optimises itself into simple method.
        if(ref != null &&
            (f = (Function)((BindExpr)ref.binder).st).methodImpl != null
            && arity == (argc = f.methodImpl.argVar)) {
            StringBuilder sig = new StringBuilder(f.capture1 ? "(" : "([");
            sig.append("Ljava/lang/Object;");
            Apply a = this; // "this" is the last argument applied, so reverse
            Code[] args = new Code[argc];
            for(int i = argc; --i > 0; a = (Apply)a.fun) {
                args[i] = a.arg;
            }
            args[0] = a.arg; // out-of-cycle as we need "a" for fun
            a.fun.gen(context);
            if(!f.capture1) {
                context.typeInsn(CHECKCAST, "[Ljava/lang/Object;");
            }
            for(int i = 0; i < argc; ++i) {
                args[i].gen(context);
                sig.append("Ljava/lang/Object;");
            }
            sig.append(")Ljava/lang/Object;");
            context.visitLine(line);
            context.methodInsn(INVOKESTATIC, f.name, f.bindName, sig.toString());
            return;
        }

        if(fun instanceof Function) {
            f = (Function)fun;
            LoadVar arg_ = new LoadVar();
            // inline direct calls
            // TODO: constants don't need a temp variable
            if(f.uncapture(arg_)) {
                arg.gen(context);
                arg_.var = context.localVarCount++;
                context.varInsn(ASTORE, arg_.var);
                f.genClosureInit(context);
                f.body.gen(context);
                return;
            }
        }

        Apply to = (arity & 1) == 0 && arity - argc > 1 ? (Apply)fun : this;
        to.fun.gen(context);
        context.visitLine(to.line);
        context.typeInsn(CHECKCAST, "ria/lang/Fun");
        if(to == this) {
            context.visitApply(arg, line);
        } else {
            to.arg.gen(context);
            arg.gen(context);
            context.visitLine(line);
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/Fun", "apply",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        }
    }

    @Override
    public Code apply(Code arg, final CType res, int line) {
        Apply a = new Apply(res, this, arg, line);
        a.arity = arity + 1;
        if(ref != null) {
            ref.arity = a.arity;
            a.ref = ref;
        }
        return a;
    }
}
