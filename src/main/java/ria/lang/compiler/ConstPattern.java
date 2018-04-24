package ria.lang.compiler;

import org.objectweb.asm.Label;
import ria.lang.compiler.code.Code;

public final class ConstPattern extends CasePattern {
    Code v;

    ConstPattern(Code value) {
        v = value;
    }

    @Override
    public void tryMatch(Context context, Label onFail, boolean preserve) {
        if (preserve) {
            context.insn(DUP);
        }
        v.gen(context);
        context.methodInsn(INVOKEVIRTUAL, "java/lang/Object",
                            "equals", "(Ljava/lang/Object;)Z");
        context.jumpInsn(IFEQ, onFail);
    }
}
