package ria.lang.compiler;

import ria.lang.compiler.code.LoadModule;

public final class TypeScope extends Scope {
    final LoadModule module;
    private final CType[] def;

    TypeScope(Scope outer, String name, CType[] typedef, LoadModule m) {
        super(outer, name, null);
        def = typedef;
        free = RiaType.NO_PARAM;
        module = m;
    }

    @Override
    CType[] typedef(boolean use) {
        if(use && module != null) {
            module.typedefUsed = true;
        }
        return def;
    }
}
