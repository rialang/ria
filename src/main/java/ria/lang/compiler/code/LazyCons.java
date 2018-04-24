package ria.lang.compiler.code;

import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;
import ria.lang.compiler.code.BinOpRef;

public final class LazyCons extends BinOpRef {
    private int line;

    public LazyCons(int line) {
        type = RiaType.LAZYCONS_TYPE;
        coreFun = "$c$d";
        polymorph = true;
        this.line = line;
    }

    @Override
    void binGen(Context context, Code arg1, Code arg2) {
        context.visitLine(line);
        context.typeInsn(NEW, "ria/lang/LazyList");
        context.insn(DUP);
        arg1.gen(context);
        arg2.gen(context);
        context.visitLine(line);
        context.typeInsn(CHECKCAST, "ria/lang/Fun");
        context.visitInit("ria/lang/LazyList", "(Ljava/lang/Object;Lria/lang/Fun;)V");
        context.forceType("ria/lang/AbstractList");
    }
}
