package ria.lang.compiler.code;

import ria.lang.compiler.CodeGen;
import ria.lang.compiler.Context;
import ria.lang.compiler.JavaType;
import ria.lang.compiler.RiaType;

public final class ClassOfExpr extends Code implements CodeGen {
    String className;

    public ClassOfExpr(JavaType what, int array) {
        type = RiaType.CLASS_TYPE;
        String cn = what.dottedName();
        if(array != 0) {
            cn = 'L' + cn + ';';
            do {
                cn = "[".concat(cn);
            } while(--array > 0);
        }
        className = cn;
    }

    @Override
    public void gen2(Context context, Code param, int line) {
        context.ldcInsn(className);
        context.methodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
        context.forceType("java/lang/Class");
    }

    @Override
    public void gen(Context context) {
        context.constant("CLASS-OF:".concat(className), new SimpleCode(this, null, RiaType.CLASS_TYPE, 0));
    }

    @Override
    public boolean flagop(int fl) {
        return (fl & STD_CONST) != 0;
    }
}
