package ria.lang.compiler.code;

import ria.lang.compiler.Binder;
import ria.lang.compiler.CType;
import ria.lang.compiler.Context;
import ria.lang.compiler.code.BindRef;

public class StaticRef extends BindRef {
    boolean method;
    private String className;
    private String fieldName;
    private int line;

    public StaticRef(String className, String fieldName, CType type, Binder binder, boolean polymorph, int line) {
        this.type = type;
        this.binder = binder;
        this.className = className;
        this.fieldName = fieldName;
        this.polymorph = polymorph;
        this.line = line;
    }

    StaticRef(String fun, CType type, boolean polymorph, int line) {
        this("ria/lang/std", fun, type, null, polymorph, line);
        method = true;
    }

    public static boolean std(Code ref, String fun) {
        if(ref instanceof StaticRef) {
            StaticRef r = (StaticRef)ref;
            return r.method && "ria/lang/std".equals(r.className) && fun.equals(r.fieldName);
        }
        return false;
    }

    @Override
    public void gen(Context context) {
        context.visitLine(line);
        String t = javaType(type);
        if(method) {
            context.methodInsn(INVOKESTATIC, className, fieldName, "()L" + t + ';');
            context.forceType(t);
        } else {
            context.fieldInsn(GETSTATIC, className, fieldName, 'L' + t + ';');
        }
    }

    @Override
    Object valueKey() {
        return (method ? "MREF:" : "SREF:") + className + '.' + fieldName;
    }

    @Override
    public boolean flagop(int fl) {
        return (fl & (DIRECT_BIND | CONST)) != 0;
    }
}
