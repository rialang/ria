package ria.lang.compiler.code;

import ria.lang.compiler.CType;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class UnitConstant extends BindRef {
    private final Object NULL = new Object();

    public UnitConstant(CType type) {
        this.type = type == null ? RiaType.UNIT_TYPE : type;
    }

    @Override
    public void gen(Context context) {
        context.insn(ACONST_NULL);
    }

    @Override
    public boolean flagop(int fl) {
        return (fl & STD_CONST) != 0;
    }

    @Override
    Object valueKey() {
        return NULL;
    }
}
