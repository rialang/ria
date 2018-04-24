package ria.lang.compiler;

import ria.lang.compiler.code.Closure;

public class Scope {
    Scope outer;
    String name;
    Binder binder;
    CType[] free;
    Closure closure;
    RiaType.ClassBinding importClass;
    RiaType.ScopeCtx ctx;

    Scope(Scope outer, String name, Binder binder) {
        this.outer = outer;
        this.name = name;
        this.binder = binder;
        ctx = outer == null ? null : outer.ctx;
    }

    CType[] typedef(boolean use) {
        return null;
    }
}
