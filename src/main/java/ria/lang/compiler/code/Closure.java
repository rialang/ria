package ria.lang.compiler.code;

public interface Closure {
    // Closures "wrap" references to the outside world.
    BindRef refProxy(BindRef code);

    void addVar(BindExpr binder);
}
