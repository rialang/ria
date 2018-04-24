package ria.lang.compiler;

import ria.lang.compiler.code.BindRef;

public interface Binder {
    BindRef getRef(int line);
}
