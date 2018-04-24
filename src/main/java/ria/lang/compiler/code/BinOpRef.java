package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.CType;
import ria.lang.compiler.Context;

public abstract class BinOpRef extends BindRef {
    boolean markTail2;
    public String coreFun;

    @Override
    public Code apply(final Code arg1, final CType res1, final int line) {
        return new Code() {
            {
                type = res1;
            }

            @Override
            public Code apply(Code arg2, CType res, int line) {
                return new Result(arg1, arg2, res);
            }

            @Override
            public void gen(Context context) {
                BinOpRef.this.gen(context);
                context.visitApply(arg1, line);
            }
        };
    }

    @Override
    public void gen(Context context) {
        context.methodInsn(INVOKESTATIC, "ria/lang/std", coreFun, "()Lria/lang/Fun;");
        context.forceType("ria/lang/Fun");
    }

    abstract void binGen(Context context, Code arg1, Code arg2);

    void binGenIf(Context context, Code arg1, Code arg2, Label to, boolean ifTrue) {
        throw new UnsupportedOperationException("binGenIf");
    }

    @Override
    public boolean flagop(int fl) {
        return (fl & STD_CONST) != 0;
    }

    class Result extends Code {
        private Code arg1;
        private Code arg2;

        Result(Code arg1, Code arg2, CType res) {
            type = res;
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public void gen(Context context) {
            binGen(context, arg1, arg2);
        }

        @Override
        void genIf(Context context, Label to, boolean ifTrue) {
            binGenIf(context, arg1, arg2, to, ifTrue);
        }

        @Override
        public void markTail() {
            if(markTail2) {
                arg2.markTail();
            }
        }
    }
}
