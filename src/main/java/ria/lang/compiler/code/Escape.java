package ria.lang.compiler.code;

import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class Escape extends IsNullPtr {
    public Escape(int line) {
        super(RiaType.WITH_EXIT_TYPE, "withExit", line);
        normalIf = true;
    }

    @Override
    void gen(Context context, Code block, int line) {
        block.gen(context);
        context.typeInsn(CHECKCAST, "ria/lang/Fun");
        context.methodInsn(INVOKESTATIC, "ria/lang/EscapeFun", "with", "(Lria/lang/Fun;)Ljava/lang/Object;");
    }
}
