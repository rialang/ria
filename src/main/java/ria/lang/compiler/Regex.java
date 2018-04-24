package ria.lang.compiler;

import ria.lang.compiler.code.BindRef;
import ria.lang.compiler.code.RegexFun;

public final class Regex implements Binder {
    private String fun, impl;
    private CType type;

    public Regex(String fun, String impl, CType type) {
        this.fun = fun;
        this.impl = impl;
        this.type = type;
    }

    @Override
    public BindRef getRef(int line) {
        return new RegexFun(fun, impl, type, this, line);
    }
}
