package ria.lang.compiler.code;

import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class Compose extends Core2 {
    public Compose(int line) {
        super("$d", RiaType.COMPOSE_TYPE, line);
        derivePolymorph = true;
    }

    @Override
    void genApply2(Context context, Code arg1, Code arg2, int line) {
        context.typeInsn(NEW, "ria/lang/Compose");
        context.insn(DUP);
        arg1.gen(context);
        arg2.gen(context);
        context.visitLine(line);
        context.visitInit("ria/lang/Compose", "(Ljava/lang/Object;Ljava/lang/Object;)V");
    }
}
