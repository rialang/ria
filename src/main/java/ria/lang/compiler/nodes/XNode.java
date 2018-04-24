package ria.lang.compiler.nodes;

public class XNode extends Node {
    public Node[] expr;
    public String doc;

    public XNode(String kind) {
        this.kind = kind;
    }

    public XNode(String kind, Node[] expr) {
        this.kind = kind;
        this.expr = expr;
    }

    public XNode(String kind, Node expr) {
        this.kind = kind;
        this.expr = new Node[] {expr};
        line = expr.line;
        col = expr.col;
    }

    public static XNode struct(Node[] fields) {
        for(int i = 0; i < fields.length; ++i) {
            IsOp op = null;
            Sym s = null;
            if(fields[i] instanceof Sym) {
                s = (Sym)fields[i];
            } else if(fields[i] instanceof IsOp) {
                op = (IsOp)fields[i];
                op.right.sym();
                s = (Sym)op.right;
            }
            if(s != null) {
                Bind bind = new Bind();
                bind.name = s.sym;
                bind.expr = s;
                bind.col = s.col;
                bind.line = s.line;
                bind.unbind = true;
                if(op != null) {
                    bind.type = op.type;
                }
                fields[i] = bind;
            }
        }
        return new XNode("struct", fields);
    }

    public static XNode lambda(Node arg, Node expr, Node name) {
        XNode lambda = new XNode("lambda", name == null
            ? new Node[] {arg, expr} : new Node[] {arg, expr, name});
        lambda.line = arg.line;
        lambda.col = arg.col;
        return lambda;
    }

    @Override
    public String str() {
        if(expr == null) {
            return "`".concat(kind);
        }
        StringBuilder buf = new StringBuilder("(`");
        buf.append(kind);
        for(Node anExpr : expr) {
            buf.append(' ');
            if(anExpr != null) {
                buf.append(anExpr.str());
            }
        }
        buf.append(')');
        return buf.toString();
    }
}
