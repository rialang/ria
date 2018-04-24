package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Binder;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;
import ria.lang.compiler.code.BindRef;

public final class BoolOpFun extends BoolBinOp implements Binder {
    boolean orOp;

    public BoolOpFun(boolean orOp) {
        this.type = RiaType.BOOLOP_TYPE;
        this.orOp = orOp;
        this.binder = this;
        markTail2 = true;
        coreFun = orOp ? "or" : "and";
    }

    @Override
    public BindRef getRef(int line) {
        return this;
    }

    @Override
    void binGen(Context context, Code arg1, Code arg2) {
        if(arg2 instanceof CompareFun) {
            super.binGen(context, arg1, arg2);
        } else {
            Label label = new Label(), end = new Label();
            arg1.genIf(context, label, orOp);
            arg2.gen(context);
            context.jumpInsn(GOTO, end);
            context.visitLabel(label);
            context.fieldInsn(GETSTATIC, "java/lang/Boolean",
                orOp ? "TRUE" : "FALSE", "Ljava/lang/Boolean;");
            context.visitLabel(end);
        }
    }

    @Override
    void binGenIf(Context context, Code arg1, Code arg2, Label to, boolean ifTrue) {
        if(orOp == ifTrue) {
            arg1.genIf(context, to, orOp);
            arg2.genIf(context, to, orOp);
        } else {
            Label noJmp = new Label();
            arg1.genIf(context, noJmp, orOp);
            arg2.genIf(context, to, !orOp);
            context.visitLabel(noJmp);
        }
    }
}
