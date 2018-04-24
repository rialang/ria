package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class InOpFun extends BoolBinOp {
    int line;

    public InOpFun(int line) {
        type = RiaType.IN_TYPE;
        this.line = line;
        polymorph = true;
        coreFun = "in";
    }

    @Override
    void binGenIf(Context context, Code arg1, Code arg2, Label to, boolean ifTrue) {
        arg2.gen(context);
        context.visitLine(line);
        arg1.gen(context);
        context.visitLine(line);
        context.methodInsn(INVOKEINTERFACE, "ria/lang/ByKey",
            "containsKey", "(Ljava/lang/Object;)Z");
        context.jumpInsn(ifTrue ? IFNE : IFEQ, to);
    }
}
