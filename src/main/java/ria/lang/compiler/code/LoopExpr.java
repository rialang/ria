package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public class LoopExpr extends AbstractClosure {
    public Code cond;
    public Code body;

    public LoopExpr() {
        this.type = RiaType.UNIT_TYPE;
    }

    @Override
    public BindRef refProxy(BindRef code) {
        return code;
    }

    @Override
    public void gen(Context context) {
        Label start = new Label();
        Label end = new Label();
        context.visitLabel(start);
        ++context.tainted;
        genClosureInit(context);
        cond.genIf(context, end, false);
        body.gen(context);
        --context.tainted;
        context.insn(POP);
        context.jumpInsn(GOTO, start);
        context.visitLabel(end);
        context.insn(ACONST_NULL);
    }
}
