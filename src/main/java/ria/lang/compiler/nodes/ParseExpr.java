package ria.lang.compiler.nodes;

import ria.lang.compiler.CompileException;
import ria.lang.compiler.RiaParser;

import java.util.Objects;

public final class ParseExpr {
    private boolean lastOp = true;
    private BinOp root = new BinOp(null, -1, false);
    private BinOp cur = root;

    private void apply(Node node) {
        BinOp apply = new BinOp("", 2, true);
        apply.line = node.line;
        apply.col = node.col;
        addOp(apply);
    }

    private void addOp(BinOp op) {
        BinOp to = cur;
        if(Objects.equals(op.op, "-") && lastOp || Objects.equals(op.op, "$") || Objects.equals(op.op, "not")) {
            if(!lastOp) {
                apply(op);
                to = cur;
            }
            if(Objects.equals(op.op, "-")) {
                op.priority = 1;
            }
            to.left = to.right;
        } else if(lastOp) {
            throw new CompileException(op, "Do not stack operators");
        } else {
            while(to.parent != null && (to.postfix || to.priority < op.priority ||
                to.priority == op.priority && op.toRight)) {
                to = to.parent;
            }
            op.right = to.right;
        }
        op.parent = to;
        to.right = op;
        cur = op;
        lastOp = !op.postfix;
    }

    public void add(Node node) {
        if(node instanceof BinOp && ((BinOp)node).parent == null &&
            (!lastOp || !Objects.equals(node.kind, "listop"))) {
            addOp((BinOp)node);
        } else {
            if(!lastOp) {
                apply(node);
            }
            lastOp = false;
            cur.left = cur.right;
            cur.right = node;
        }
    }

    public Node result() {
        if(cur.left == null && cur.priority != -1 && cur.priority != 1 &&
            cur.priority != RiaParser.NOT_OP_LEVEL &&
            !cur.postfix || cur.right == null) {
            throw new CompileException(cur, "Expecting some value" + cur);
        }
        return root.right;
    }
}
