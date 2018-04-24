package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class Same extends BoolBinOp {
    public Same() {
        type = RiaType.EQ_TYPE;
        polymorph = true;
        coreFun = "same$q";
    }

    @Override
    void binGenIf(Context context, Code arg1, Code arg2,
                  Label to, boolean ifTrue) {
        arg1.gen(context);
        arg2.gen(context);
        context.jumpInsn(ifTrue ? IF_ACMPEQ : IF_ACMPNE, to);
    }
}
