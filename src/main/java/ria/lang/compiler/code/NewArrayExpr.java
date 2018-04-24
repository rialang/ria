package ria.lang.compiler.code;

import ria.lang.compiler.CType;
import ria.lang.compiler.Context;
import ria.lang.compiler.JavaType;
import ria.lang.compiler.RiaType;

public final class NewArrayExpr extends Code {
    private Code count;
    private int line;

    public NewArrayExpr(CType type, Code count, int line) {
        this.type = type;
        this.count = count;
        this.line = line;
    }

    @Override
    public void gen(Context context) {
        if(count != null) {
            count.genInt(context, line, false);
        }
        context.visitLine(line);
        if(type.param[0].type != RiaType.JAVA) { // array of arrays
            context.typeInsn(ANEWARRAY, JavaType.descriptionOf(type.param[0]));
            return;
        }
        JavaType jt = type.param[0].javaType;
        int t;
        switch(jt.description.charAt(0)) {
            case 'B':
                t = T_BYTE;
                break;
            case 'C':
                t = T_CHAR;
                break;
            case 'D':
                t = T_DOUBLE;
                break;
            case 'F':
                t = T_FLOAT;
                break;
            case 'I':
                t = T_INT;
                break;
            case 'J':
                t = T_LONG;
                break;
            case 'S':
                t = T_SHORT;
                break;
            case 'Z':
                t = T_BOOLEAN;
                break;
            case 'L':
                context.typeInsn(ANEWARRAY, jt.className());
                return;
            default:
                throw new IllegalStateException("ARRAY<" + jt.description + '>');
        }
        context.visitIntInsn(NEWARRAY, t);
    }
}
