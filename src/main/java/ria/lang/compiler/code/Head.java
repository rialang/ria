package ria.lang.compiler.code;

import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class Head extends IsNullPtr {
    public Head(int line) {
        super(RiaType.LIST_TO_A, "head", line);
    }

    @Override
    void gen(Context context, Code arg, int line) {
        arg.gen(context);
        context.visitLine(line);
        context.typeInsn(CHECKCAST, "ria/lang/AbstractList");
        context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractList",
            "first", "()Ljava/lang/Object;");
    }
}
