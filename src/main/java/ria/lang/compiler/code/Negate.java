package ria.lang.compiler.code;

import ria.lang.compiler.CType;
import ria.lang.compiler.CodeGen;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;
import ria.lang.compiler.code.NumericConstant;
import ria.lang.compiler.code.SimpleCode;

public final class Negate extends StaticRef implements CodeGen {
    public Negate() {
        super("negate", RiaType.NUM_TO_NUM, false, 0);
    }

    @Override
    public void gen2(Context context, Code arg, int line) {
        arg.gen(context);
        context.visitLine(line);
        context.typeInsn(CHECKCAST, "ria/lang/RiaNum");
        context.ldcInsn(0L);
        context.methodInsn(INVOKEVIRTUAL, "ria/lang/RiaNum",
            "subFrom", "(J)Lria/lang/RiaNum;");
        context.forceType("ria/lang/RiaNum");
    }

    @Override
    public Code apply(final Code arg1, final CType res1, final int line) {
        if(arg1 instanceof NumericConstant) {
            return new NumericConstant(((NumericConstant)arg1)
                .num.subFrom(0));
        }
        return new SimpleCode(this, arg1, RiaType.NUM_TYPE, line);
    }
}
