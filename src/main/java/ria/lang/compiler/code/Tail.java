package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class Tail extends IsNullPtr {
    public Tail(int line) {
        super(RiaType.LIST_TO_LIST, "tail", line);
    }

    @Override
    void gen(Context context, Code arg, int line) {
        arg.gen(context);
        context.visitLine(line);
        context.typeInsn(CHECKCAST, "ria/lang/AbstractList");
        context.insn(DUP);
        Label end = new Label();
        context.jumpInsn(IFNULL, end);
        context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractList", "rest", "()Lria/lang/AbstractList;");
        context.visitLabel(end);
        context.forceType("ria/lang/AbstractList");
    }
}
