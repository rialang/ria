package ria.lang.compiler;

import org.objectweb.asm.Label;

public abstract class AbstractListPattern extends CasePattern {
    static final CasePattern EMPTY_PATTERN = new CasePattern() {
        @Override
        public void tryMatch(Context context, Label onFail, boolean preserve) {
            context.insn(DUP);
            Label cont = new Label();
            context.jumpInsn(IFNULL, cont);
            if (preserve) {
                context.insn(DUP);
            }
            context.typeInsn(CHECKCAST, "ria/lang/AbstractIterator");
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractIterator",
                                "isEmpty", "()Z");
            context.jumpInsn(IFEQ, onFail);
            if (preserve) {
                context.visitLabel(cont);
            } else {
                Label end = new Label();
                context.jumpInsn(GOTO, end);
                context.visitLabel(cont);
                context.insn(POP);
                context.visitLabel(end);
            }
        }
    };

    abstract boolean listMatch(Context context, Label onFail, Label dropFail);

    @Override
    public void tryMatch(Context context, Label onFail, boolean preserve) {
        Label dropFail = preserve ? onFail : new Label();
        context.insn(DUP);
        context.jumpInsn(IFNULL, dropFail);
        context.typeInsn(CHECKCAST, "ria/lang/AbstractList");
        context.insn(DUP);
        context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractList",
                            "isEmpty", "()Z");
        context.jumpInsn(IFNE, dropFail);
        if (preserve) {
            context.insn(DUP);
            dropFail = new Label();
        }
        if (listMatch(context, onFail, dropFail) || !preserve) {
            Label cont = new Label();
            context.jumpInsn(GOTO, cont);
            context.visitLabel(dropFail);
            context.insn(POP);
            context.jumpInsn(GOTO, onFail);
            context.visitLabel(cont);
        }
    }
}
