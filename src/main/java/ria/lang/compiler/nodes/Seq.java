package ria.lang.compiler.nodes;

import ria.lang.compiler.CompileException;
import ria.lang.compiler.RiaParser;

import java.util.Objects;

public final class Seq extends Node {
    public static final Object EVAL = new Object();

    public Node[] st;
    public Object seqKind;

    public Seq(Node[] st, Object kind) {
        this.st = st;
        this.seqKind = kind;
    }

    @Override
    public String str() {
        StringBuilder res = new StringBuilder("(`begin");
        if(seqKind != null) {
            res.append(':').append(seqKind);
        }
        for(int i = 0; st != null && i < st.length; ++i) {
            res.append(' ').append(st[i].str());
        }
        res.append(')');
        return res.toString();
    }

    public void checkBind() {
        if(st != null && st.length >= 2) {
            for(int i = 0; i < (st.length - 1); i++) {
                BinOp bo;
                BinOp ref;
                if(!(this.st[i] instanceof BinOp)
                    || !Objects.equals((bo = (BinOp)st[i]).op, "<-")
                    || bo.right == null) {
                    continue;
                }

                Node bindFn;
                Sym argName;
                if(bo.left instanceof BinOp
                    && Objects.equals((ref = (BinOp)bo.left).op, RiaParser.FIELD_OP)
                    && ref.right instanceof Sym) {

                    // argName and bindFunction
                    argName = (Sym)ref.right;

                    bindFn = ref;
                    ref.right = new Sym("bind");
                    ref.right.pos(bindFn.line, bindFn.col);
                } else if(bo.left instanceof Sym) {
                    argName = (Sym)bo.left;
                    bindFn = new Sym("bind");
                    bindFn.pos(argName.line, argName.col);
                } else {
                    continue;
                }

                // Create a lambda for the bind
                Node[] body = new Node[this.st.length - i - 1];
                System.arraycopy(this.st, i + 1, body, 0, body.length);
                Seq bodySeq = new Seq(body, null);
                bodySeq.checkBind();
                XNode lam = XNode.lambda(argName, bodySeq, null);
                lam.pos(bo.line, bo.col);

                // Add the new nodes of this sequence
                Node[] nst = new Node[i + 1];
                System.arraycopy(this.st, 0, nst, 0, i);

                // Add in the apply of the bind function to the lamdba
                BinOp bindOp = new BinOp("", 2, true);
                bindOp.left = bindFn;
                bindOp.right = lam;
                bindOp.pos(bo.line, bo.col);

                // Add the apply of (bind lamdba) to the value
                BinOp apl = new BinOp("", 2, true);
                apl.pos(bo.line, bo.col);
                apl.left = bindOp;
                apl.right = bo.right;
                nst[i] = apl;
                this.st = nst;
            }
        } else if(st != null && st.length == 1) {
            BinOp bo;
            if(st[0] instanceof BinOp
                && Objects.equals((bo = (BinOp)st[0]).op, "<-")
                && bo.right != null) {
                throw new CompileException(st[0], "Bind operator can only be used in a sequence");
            }
        }
    }
}
