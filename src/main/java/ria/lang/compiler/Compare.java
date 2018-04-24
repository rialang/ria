package ria.lang.compiler;

import ria.lang.compiler.code.BindRef;
import ria.lang.compiler.code.Code;
import ria.lang.compiler.code.CompareFun;

public final class Compare implements Binder {
    CType type;
    int op;
    String fun;

    public Compare(CType type, int op, String fun) {
        this.op = op;
        this.type = type;
        this.fun = Code.mangle(fun);
    }

    @Override
    public BindRef getRef(int line) {
        CompareFun c = new CompareFun();
        c.binder = this;
        c.type = type;
        c.op = op;
        c.polymorph = true;
        c.line = line;
        c.coreFun = fun;
        return c;
    }
}
