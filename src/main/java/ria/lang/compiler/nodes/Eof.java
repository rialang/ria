package ria.lang.compiler.nodes;

public final class Eof extends XNode {
    public Eof(String kind) {
        super(kind);
    }

    public String toString() {
        return kind;
    }
}
