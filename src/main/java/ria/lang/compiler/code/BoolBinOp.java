package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Context;
import ria.lang.compiler.code.BinOpRef;

public abstract class BoolBinOp extends BinOpRef {
    @Override
    void binGen(Context context, Code arg1, Code arg2) {
        Label label = new Label();
        binGenIf(context, arg1, arg2, label, false);
        context.genBoolean(label);
    }
}
