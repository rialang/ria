package ria.lang.compiler.nodes;

import ria.lang.compiler.RiaParser;

import java.util.Objects;

public class BinOp extends Node {
    int priority;
    public String op;
    boolean toRight;
    public boolean postfix;
    public Node left;
    public Node right;
    public BinOp parent;

    public BinOp(String op, int priority, boolean toRight) {
        this.op = op;
        this.priority = priority;
        this.toRight = toRight;
    }

    @Override
    public String str() {
        StringBuilder s = new StringBuilder().append('(');
        if(left == null) {
            s.append("`flip ");
        }
        if(!Objects.equals(op, "")) {
            s.append(Objects.equals(op, RiaParser.FIELD_OP) ? "`." : op).append(' ');
        }
        if(left != null) {
            s.append(left.str()).append(' ');
        }
        if(right != null) {
            s.append(right.str());
        }
        return s.append(')').toString();
    }
}
