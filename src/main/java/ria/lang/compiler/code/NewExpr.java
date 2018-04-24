package ria.lang.compiler.code;

import ria.lang.compiler.Context;
import ria.lang.compiler.JavaType;
import ria.lang.compiler.RiaType;

public final class NewExpr extends JavaExpr {
    private RiaType.ClassBinding extraArgs;

    public NewExpr(JavaType.Method init, Code[] args,
                   RiaType.ClassBinding extraArgs, int line) {
        super(null, init, args, line);
        type = init.classType;
        this.extraArgs = extraArgs;
    }

    @Override
    public void gen(Context context) {
        String name = method.classType.javaType.className();
        context.typeInsn(NEW, name);
        context.insn(DUP);
        genCall(context, extraArgs.getCaptures(), INVOKESPECIAL);
        context.forceType(name);
    }
}
