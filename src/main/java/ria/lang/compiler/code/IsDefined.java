package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class IsDefined extends IsNullPtr {
    public IsDefined(int line) {
        super(RiaType.A_TO_BOOL, "defined$q", line);
    }

    @Override
    void genIf(Context context, Code arg, Label to, boolean ifTrue, int line) {
        Label isNull = new Label(), end = new Label();
        arg.gen(context);
        context.insn(DUP);
        context.jumpInsn(IFNULL, isNull);
        context.fieldInsn(GETSTATIC, "ria/lang/Core",
            "UNDEF_STR", "Ljava/lang/String;");
        context.jumpInsn(IF_ACMPEQ, ifTrue ? end : to);
        context.jumpInsn(GOTO, ifTrue ? to : end);
        context.visitLabel(isNull);
        context.insn(POP);
        if(!ifTrue) {
            context.jumpInsn(GOTO, to);
        }
        context.visitLabel(end);
    }
}
