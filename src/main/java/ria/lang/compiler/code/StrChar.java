package ria.lang.compiler.code;

import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class StrChar extends BinOpRef {
    private int line;

    public StrChar(int line) {
        coreFun = "strChar";
        type = RiaType.STR_TO_NUM_TO_STR;
        this.line = line;
    }

    @Override
    void binGen(Context context, Code arg1, Code arg2) {
        arg1.gen(context);
        context.typeInsn(CHECKCAST, "java/lang/String");
        arg2.genInt(context, line, false);
        context.insn(DUP);
        context.intConst(1);
        context.insn(IADD);
        context.methodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;");
        context.forceType("java/lang/String");
    }
}
