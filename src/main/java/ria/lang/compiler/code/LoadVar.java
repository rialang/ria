package ria.lang.compiler.code;

import ria.lang.compiler.Context;

public final class LoadVar extends Code {
    int var;

    @Override
    public void gen(Context context) {
        context.varInsn(ALOAD, var);
    }
}
