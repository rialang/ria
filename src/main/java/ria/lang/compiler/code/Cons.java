package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;
import ria.lang.compiler.code.BinOpRef;

public final class Cons extends BinOpRef {
    private int line;

    public Cons(int line) {
        type = RiaType.CONS_TYPE;
        coreFun = "$c$c";
        polymorph = true;
        this.line = line;
    }

    @Override
    void binGen(Context context, Code arg1, Code arg2) {
        context.visitLine(line);
        context.typeInsn(NEW, "ria/lang/RiaList");
        context.insn(DUP);
        arg1.gen(context);
        arg2.gen(context);
        context.visitLine(line);
        context.typeInsn(CHECKCAST, "ria/lang/AbstractList");
        if(arg2.type.deref().param[1].deref() != RiaType.NO_TYPE) {
            Label cons = new Label();
            context.insn(DUP);
            context.jumpInsn(IFNULL, cons); // null, ok
            context.insn(DUP);
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractList", "isEmpty", "()Z");
            context.jumpInsn(IFEQ, cons); // not empty, ok
            context.insn(POP); // empty not-null, dump it
            context.insn(ACONST_NULL); // and use null instead
            context.visitLabel(cons);
        }
        context.visitInit("ria/lang/RiaList", "(Ljava/lang/Object;Lria/lang/AbstractList;)V");
        context.forceType("ria/lang/AbstractList");
    }
}
