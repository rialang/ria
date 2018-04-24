package ria.lang.compiler.nodes;

import ria.lang.compiler.RiaParser;

public final class InstanceOf extends BinOp {
    public String className;

    public InstanceOf(String className) {
        super("instanceof", RiaParser.COMP_OP_LEVEL, true);
        postfix = true;
        this.className = className;
    }
}
