package ria.lang.compiler.code;

import ria.lang.compiler.CType;
import ria.lang.compiler.CodeGen;
import ria.lang.compiler.Context;

import java.util.Arrays;

public abstract class Core2 extends StaticRef {
    boolean derivePolymorph;

    Core2(String coreFun, CType type, int line) {
        super(coreFun, type, true, line);
    }

    @Override
    public Code apply(final Code arg1, CType res, int line1) {
        return new Apply(res, this, arg1, line1) {
            @Override
            public Code apply(final Code arg2, final CType res, final int line2) {
                class A extends Code implements CodeGen {
                    @Override
                    public void gen2(Context ctx, Code param, int line) {
                        genApply2(ctx, arg1, arg2, line2);
                    }

                    @Override
                    public void gen(Context ctx) {
                        if(prepareConst(ctx)) {
                            Object[] key = {Core2.this.getClass(), arg1.valueKey(), arg2.valueKey()};
                            ctx.constant(Arrays.asList(key), new SimpleCode(this, null, type, 0));
                        } else {
                            genApply2(ctx, arg1, arg2, line2);
                        }
                    }

                    @Override
                    public boolean flagop(int fl) {
                        return derivePolymorph && (fl & (CONST | PURE)) != 0 &&
                            arg1.flagop(fl) && arg2.flagop(fl);
                    }

                    @Override
                    boolean prepareConst(Context ctx) {
                        return derivePolymorph && arg1.prepareConst(ctx) && arg2.prepareConst(ctx);
                    }
                }
                A r = new A();
                r.type = res;
                r.polymorph = derivePolymorph && arg1.polymorph && arg2.polymorph;
                return r;
            }
        };
    }

    abstract void genApply2(Context context, Code arg1, Code arg2, int line);
}
