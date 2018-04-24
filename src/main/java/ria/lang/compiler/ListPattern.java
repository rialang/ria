package ria.lang.compiler;

import org.objectweb.asm.Label;

public final class ListPattern extends AbstractListPattern {
    private CasePattern[] items;

    ListPattern(CasePattern[] items) {
        this.items = items;
    }

    @Override
    boolean listMatch(Context context, Label onFail, Label dropFail) {
        boolean dropUsed = false;
        for (int i = 0; i < items.length; ++i) {
            if (i != 0) {
                context.insn(DUP);
                context.jumpInsn(IFNULL, dropFail);
                dropUsed = true;
            }
            if (items[i] != ANY_PATTERN) {
                context.insn(DUP);
                context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractIterator",
                                    "first", "()Ljava/lang/Object;");
                items[i].preparePattern(context);
                items[i].tryMatch(context, dropFail, false);
                dropUsed |= !items[i].irrefutable();
            }
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractIterator",
                                "next", "()Lria/lang/AbstractIterator;");
        }
        context.jumpInsn(IFNONNULL, onFail);
        return dropUsed;
    }
}
