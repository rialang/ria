package ria.lang.compiler.nodes;

public final class Sym extends Node {
    public String sym;

    public Sym(String sym) {
        this.sym = sym;
    }

    @Override
    public String sym() {
        return sym;
    }

    @Override
    public String str() {
        return sym;
    }

    public String toString() {
        return sym;
    }
}
