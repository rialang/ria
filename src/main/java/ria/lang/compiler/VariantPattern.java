package ria.lang.compiler;

import org.objectweb.asm.*;

public final class VariantPattern extends CasePattern {
    String variantTag;
    CasePattern variantArg;

    VariantPattern(String tagName, CasePattern arg) {
        variantTag = tagName;
        variantArg = arg;
    }

    @Override
    public int preparePattern(Context context) {
        context.typeInsn(CHECKCAST, "ria/lang/Tag");
        context.insn(DUP);
        context.fieldInsn(GETFIELD, "ria/lang/Tag", "name",
                           "Ljava/lang/String;");
        return 2; // TN
    }

    @Override
    public void tryMatch(Context context, Label onFail, boolean preserve) {
        if (preserve) {
            context.insn(DUP); // TNN
            context.ldcInsn(variantTag);
            context.jumpInsn(IF_ACMPNE, onFail); // TN
            if (variantArg == ANY_PATTERN) {
                return;
            }
            context.insn(SWAP); // NT
            context.insn(DUP_X1); // TNT
        } else if (variantArg == ANY_PATTERN) {
            context.insn(SWAP); // NT
            context.insn(POP); // N
            context.ldcInsn(variantTag);
            context.jumpInsn(IF_ACMPNE, onFail);
            return;
        } else {
            Label cont = new Label(); // TN
            context.ldcInsn(variantTag);
            context.jumpInsn(IF_ACMPEQ, cont); // T
            context.insn(POP);
            context.jumpInsn(GOTO, onFail);
            context.visitLabel(cont);
        }
        context.fieldInsn(GETFIELD, "ria/lang/Tag", "value",
                             "Ljava/lang/Object;"); // TNt (t)
        variantArg.preparePattern(context);
        variantArg.tryMatch(context, onFail, false); // TN ()
    }
}

