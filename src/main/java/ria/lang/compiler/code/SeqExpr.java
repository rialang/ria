package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Context;

public class SeqExpr extends Code {
    public Code st;
    public Code result;

    public SeqExpr(Code statement) {
        st = statement;
    }

    @Override
    public void gen(Context context) {
        st.gen(context);
        context.insn(POP); // ignore the result of st expr
        result.gen(context);
    }

    @Override
    void genIf(Context context, Label to, boolean ifTrue) {
        st.gen(context);
        context.insn(POP); // ignore the result of st expr
        result.genIf(context, to, ifTrue);
    }

    @Override
    public void markTail() {
        result.markTail();
    }
}
