package ria.lang.compiler.nodes;

import ria.lang.Core;

public final class Str extends Node {
    public String str;

    public Str(String str) {
        this.str = str;
    }

    @Override
    public String str() {
        return Core.show(str);
    }
}
