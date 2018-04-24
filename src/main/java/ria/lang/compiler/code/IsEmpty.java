package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class IsEmpty extends IsNullPtr {
    public IsEmpty(int line) {
        super(RiaType.MAP_TO_BOOL, "empty$q", line);
    }

    @Override
    void genIf(Context context, Code arg, Label to, boolean ifTrue, int line) {
        Label isNull = new Label(), end = new Label();
        arg.gen(context);
        context.visitLine(line);
        context.insn(DUP);
        context.jumpInsn(IFNULL, isNull);
        context.methodInsn(INVOKEINTERFACE, "ria/lang/Collection", "isEmpty", "()Z");
        context.jumpInsn(IFNE, ifTrue ? to : end);
        context.jumpInsn(GOTO, ifTrue ? end : to);
        context.visitLabel(isNull);
        context.insn(POP);
        if(ifTrue) {
            context.jumpInsn(GOTO, to);
        }
        context.visitLabel(end);
    }
}
