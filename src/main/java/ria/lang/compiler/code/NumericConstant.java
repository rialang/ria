package ria.lang.compiler.code;

import ria.lang.BigNum;
import ria.lang.IntNum;
import ria.lang.RiaNum;
import ria.lang.RatNum;
import ria.lang.compiler.CodeGen;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class NumericConstant extends Code implements CodeGen {
    RiaNum num;

    public NumericConstant(RiaNum num) {
        type = RiaType.NUM_TYPE;
        this.num = num;
    }

    @Override
    public boolean flagop(int fl) {
        return ((fl & INT_NUM) != 0 && num instanceof IntNum) ||
            (fl & STD_CONST) != 0;
    }

    @Override
    void genInt(Context context, int lineno, boolean longValue) {
        if(longValue) {
            context.ldcInsn(num.longValue());
        } else {
            context.intConst(num.intValue());
        }
    }

    @Override
    public void gen2(Context context, Code param, int line) {
        context.typeInsn(NEW, "ria/lang/RatNum");
        context.insn(DUP);
        RatNum rat = ((RatNum)num).reduce();
        context.intConst(rat.numerator());
        context.intConst(rat.denominator());
        context.visitInit("ria/lang/RatNum", "(II)V");
    }

    @Override
    public void gen(Context context) {
        if(context.constants.constants.containsKey(num)) {
            context.constant(num, this);
            return;
        }
        if(num instanceof RatNum) {
            context.constant(num, new SimpleCode(this, null, RiaType.NUM_TYPE, 0));
            return;
        }
        Impl v = new Impl();

        if(num instanceof IntNum) {
            v.jtype = "ria/lang/IntNum";
            // Optimise some common numbers
            if(IntNum.__1.compareTo(num) <= 0 &&
                IntNum._9.compareTo(num) >= 0) {
                context.fieldInsn(GETSTATIC, v.jtype,
                    IntNum.__1.equals(num) ? "__1" :
                        IntNum.__2.equals(num) ? "__2" : "_" + num,
                    "Lria/lang/IntNum;");
                context.forceType("ria/lang/RiaNum");
                return;
            }
            v.val = num.longValue();
            v.sig = "(J)V";
        } else if(num instanceof BigNum) {
            v.jtype = "ria/lang/BigNum";
            v.val = num.toString();
            v.sig = "(Ljava/lang/String;I)V";
        } else {
            v.jtype = "ria/lang/FloatNum";
            v.val = num.doubleValue();
            v.sig = "(D)V";
        }
        v.type = RiaType.NUM_TYPE;
        context.constant(num, v);
    }

    @Override
    Object valueKey() {
        return num;
    }

    private static final class Impl extends Code {
        String jtype, sig;
        Object val;

        @Override
        public void gen(Context context) {
            context.typeInsn(NEW, jtype);
            context.insn(DUP);
            context.ldcInsn(val);
            if(val instanceof String) {
                context.intConst(10);
            }
            context.visitInit(jtype, sig);
        }
    }
}
