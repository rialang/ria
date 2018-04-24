package ria.lang.compiler.code;

import ria.lang.compiler.CType;
import ria.lang.compiler.CodeGen;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public class SimpleCode extends Code {
    private Code param;
    private int line;
    private CodeGen impl;

    public SimpleCode(CodeGen impl, Code param, CType type, int line) {
        this.impl = impl;
        this.param = param;
        this.line = line;
        this.type = type == null ? RiaType.UNIT_TYPE : type;
    }

    @Override
    public void gen(Context context) {
        impl.gen2(context, param, line);
    }
}
