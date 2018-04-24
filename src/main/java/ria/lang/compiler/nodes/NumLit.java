package ria.lang.compiler.nodes;

import ria.lang.RiaNum;

public final class NumLit extends Node {
    public RiaNum num;

    public NumLit(RiaNum num) {
        this.num = num;
    }

    @Override
    public String str() {
        return String.valueOf(num);
    }
}
