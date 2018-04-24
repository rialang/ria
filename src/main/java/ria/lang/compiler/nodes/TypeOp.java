package ria.lang.compiler.nodes;

import ria.lang.compiler.RiaParser;

public class TypeOp extends BinOp {
    public TypeNode type;

    public TypeOp(String what, TypeNode type) {
        super(what, RiaParser.IS_OP_LEVEL, true);
        postfix = true;
        this.type = type;
    }

    @Override
    public String str() {
        return "(`" + op + ' ' + (right == null ? "()" : right.str())
            + ' ' + type.str() + ')';
    }
}
