package ria.lang.compiler.nodes;

import ria.lang.compiler.CompileException;
import ria.lang.compiler.RiaParser;

import java.util.List;
import java.util.Objects;

public final class Bind extends Node {
    public String name;
    public Node expr;
    public TypeNode type;
    public boolean mutable;
    public boolean property;
    public boolean unbind;
    public String doc;

    public Bind() {
    }

    public Bind(List<Node> args, Node expr, boolean inStruct, String doc) {
        String s;
        this.doc = doc;
        int first = 0;
        Node nameNode = null;
        while(first < args.size()) {
            nameNode = args.get(first);
            ++first;
            if(Objects.equals(nameNode.kind, "var")) {
                mutable = true;
            } else if(Objects.equals(nameNode.kind, "let")) {
                mutable = false;
            } else if(Objects.equals(nameNode.kind, "unbind")) {
                unbind = true;
            } else {
                break;
            }
        }
        if(!mutable && nameNode instanceof Sym) {
            s = ((Sym)nameNode).sym;
            if(inStruct && args.size() > first) {
                if(Objects.equals(s, "get")) {
                    property = true;
                    nameNode = args.get(first++);
                } else if(Objects.equals(s, "set")) {
                    property = true;
                    mutable = true;
                    nameNode = args.get(first++);
                }
            }
        }
        if(first == 0 || first > args.size()) {
            throw new CompileException(nameNode, "Variable name is missing");
        }
        if(inStruct && Objects.equals(nameNode.kind, "``")) {
            nameNode = ((XNode)nameNode).expr[0];
        }
        if(!(nameNode instanceof Sym)) {
            throw new CompileException(nameNode, Objects.equals(nameNode.kind, "class")
                ? "Missing ; after class definition"
                : "Illegal binding name: " + nameNode
                + " (missing ; after expression?)");
        }
        line = nameNode.line;
        col = nameNode.col;
        this.name = ((Sym)nameNode).sym;
        if(first < args.size() && args.get(first) instanceof BinOp &&
            (Objects.equals(s = ((BinOp)args.get(first)).op, RiaParser.FIELD_OP) || Objects.equals(s, "::"))) {
            throw new CompileException(args.get(first),
                "Bad argument on binding (use := for assignment, not =)");
        }
        int i = args.size() - 1;
        if(i >= first && args.get(i) instanceof IsOp) {
            type = ((IsOp)args.get(i)).type;
            --i;
        }
        for(; i >= first; --i) {
            expr = XNode.lambda(args.get(i), expr,
                i == first ? nameNode : null);
        }
        this.expr = expr;
    }

    @Override
    public String str() {
        StringBuilder s = new StringBuilder("(`let ");
        if(doc != null) {
            s.append("/**");
            s.append(doc);
            s.append(" */ ");
        }
        if(unbind) {
            s.append("`unbind ");
        }
        if(property) {
            s.append(mutable ? "`set " : "`get ");
        } else if(mutable) {
            s.append("`mutable ");
        }
        s.append(name);
        s.append(' ');
        s.append(expr.str());
        s.append(')');
        return s.toString();
    }
}
