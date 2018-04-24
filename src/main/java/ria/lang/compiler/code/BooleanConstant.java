package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;
import ria.lang.compiler.code.BindRef;

public final class BooleanConstant extends BindRef {
    private boolean val;

    public BooleanConstant(boolean val) {
        type = RiaType.BOOL_TYPE;
        this.val = val;
    }

    @Override
    public boolean flagop(int fl) {
        return (fl & STD_CONST) != 0;
    }

    @Override
    public void gen(Context context) {
        context.fieldInsn(GETSTATIC, "java/lang/Boolean",
            val ? "TRUE" : "FALSE", "Ljava/lang/Boolean;");
    }

    @Override
    void genIf(Context context, Label to, boolean ifTrue) {
        if(val == ifTrue) {
            context.jumpInsn(GOTO, to);
        }
    }

    @Override
    Object valueKey() {
        return val;
    }
}
