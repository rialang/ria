package ria.lang.compiler;

import org.objectweb.asm.Label;

public final class ConsPattern extends AbstractListPattern {
    private CasePattern hd;
    private CasePattern tl;

    ConsPattern(CasePattern hd, CasePattern tl) {
        this.hd = hd;
        this.tl = tl;
    }

    @Override
    boolean listMatch(Context context, Label onFail, Label dropFail) {
        if (hd != ANY_PATTERN) {
            if (tl != ANY_PATTERN) {
                context.insn(DUP);
            } else {
                dropFail = onFail;
            }
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractList",
                                "first", "()Ljava/lang/Object;");
            hd.preparePattern(context);
            hd.tryMatch(context, dropFail, false);
        }
        if (tl != ANY_PATTERN) {
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractList",
                                "rest", "()Lria/lang/AbstractList;");
            tl.preparePattern(context);
            tl.tryMatch(context, onFail, false);
        } else if (hd == ANY_PATTERN) {
            context.insn(POP);
        }
        return dropFail != onFail && !hd.irrefutable();
    }
}
