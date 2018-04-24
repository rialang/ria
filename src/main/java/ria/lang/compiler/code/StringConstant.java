package ria.lang.compiler.code;

import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class StringConstant extends Code {
    public String str;

    public StringConstant(String str) {
        type = RiaType.STR_TYPE;
        this.str = str;
    }

    @Override
    public void gen(Context context) {
        context.ldcInsn(str);
    }

    @Override
    public boolean flagop(int fl) {
        return (fl & STD_CONST) != 0;
    }

    @Override
    Object valueKey() {
        return str;
    }
}
