package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Context;
import ria.lang.compiler.JavaType;
import ria.lang.compiler.RiaType;

public final class InstanceOfExpr extends Code {
    Code expr;
    String className;

    public InstanceOfExpr(Code expr, JavaType what) {
        type = RiaType.BOOL_TYPE;
        this.expr = expr;
        className = what.className();
    }

    @Override
    void genIf(Context context, Label to, boolean ifTrue) {
        expr.gen(context);
        context.typeInsn(INSTANCEOF, className);
        context.jumpInsn(ifTrue ? IFNE : IFEQ, to);
    }

    @Override
    public void gen(Context context) {
        Label label = new Label();
        genIf(context, label, false);
        context.genBoolean(label);
    }
}
