package ria.lang.compiler;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import ria.lang.compiler.code.BindRef;
import ria.lang.compiler.code.CaseExpr;

public final class BindPattern extends CasePattern implements Binder {
    private CaseExpr caseExpr;
    private int nth;

    BindRef param = new BindRef() {
        @Override
        public void gen(Context context) {
            context.load(caseExpr.paramStart + nth);
        }
    };

    public BindPattern(CaseExpr caseExpr, CType type) {
        this.caseExpr = caseExpr;
        param.binder = this;
        param.type = type;
        nth = caseExpr.paramCount++;
    }

    @Override
    public BindRef getRef(int line) {
        return param;
    }

    @Override
    public void tryMatch(Context context, Label onFail, boolean preserve) {
        if (preserve) {
            context.insn(Opcodes.DUP);
        }
        context.varInsn(Opcodes.ASTORE, caseExpr.paramStart + nth);
    }

    @Override
    boolean irrefutable() {
        return true;
    }
}
