package ria.lang.compiler;

import ria.lang.compiler.code.ArithOpFun;
import ria.lang.compiler.code.BindRef;
import ria.lang.compiler.code.Code;

public final class ArithOp implements Binder {
    private String fun;
    private String method;
    private CType type;

    public ArithOp(String op, String method, CType type) {
        fun = Code.mangle(op);
        this.method = method;
        this.type = type;
    }

    @Override
    public BindRef getRef(int line) {
        return new ArithOpFun(fun, method, type, this, line);
    }
}
