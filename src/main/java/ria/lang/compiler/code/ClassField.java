package ria.lang.compiler.code;


import ria.lang.compiler.CodeGen;
import ria.lang.compiler.Context;
import ria.lang.compiler.JavaType;
import ria.lang.compiler.RiaType;
import ria.lang.compiler.code.JavaExpr;
import ria.lang.compiler.code.SimpleCode;

public final class ClassField extends JavaExpr implements CodeGen {
    private JavaType.Field field;

    public ClassField(Code object, JavaType.Field field, int line) {
        super(object, null, null, line);
        this.type = field.convertedType();
        this.field = field;
    }

    @Override
    public void gen(Context context) {
        JavaType classType = field.classType.javaType;
        if(object != null) {
            object.gen(context);
            classType = object.type.deref().javaType;
        }
        context.visitLine(line);
        String descr = JavaType.descriptionOf(field.type);
        String className = classType.className();
        if(object != null) {
            context.typeInsn(CHECKCAST, className);
        }
        // TODO: not checking for package access. check this shouldn't matter.
        if((field.access & ACC_PROTECTED) != 0
            && classType.implementation != null
            && !object.flagop(DIRECT_THIS)) {
            // TODO: object can't be null, we reference it in the condition
            descr = (object == null ? "()" : '(' + classType.description + ')') + descr;
            String name = classType.implementation.getAccessor(field, descr, false);
            context.methodInsn(INVOKESTATIC, className, name, descr);
        } else {
            context.fieldInsn(object == null ? GETSTATIC : GETFIELD,
                className, field.name, descr);
        }
        convertValue(context, field.type);
    }

    @Override
    public void gen2(Context context, Code setValue, int __) {
        JavaType classType = field.classType.javaType;
        String className = classType.className();
        if(object != null) {
            object.gen(context);
            context.typeInsn(CHECKCAST, className);
            classType = object.type.deref().javaType;
        }
        genValue(context, setValue, field.type, line);
        String descr = JavaType.descriptionOf(field.type);
        if(descr.length() > 1) {
            context.typeInsn(CHECKCAST, field.type.type == RiaType.JAVA ? field.type.javaType.className() : descr);
        }

        if((field.access & ACC_PROTECTED) != 0
            && classType.implementation != null
            && !object.flagop(DIRECT_THIS)) {
            // TODO: object can't be null, we reference it in the condition
            descr = (object != null ? "(".concat(classType.description) : "(") + descr + ")V";
            String name = classType.implementation.getAccessor(field, descr, true);
            context.methodInsn(INVOKESTATIC, className, name, descr);
        } else {
            context.fieldInsn(object == null ? PUTSTATIC : PUTFIELD, className, field.name, descr);
        }
        context.insn(ACONST_NULL);
    }

    @Override
    public Code assign(final Code setValue) {
        if((field.access & ACC_FINAL) != 0) {
            return null;
        }
        return new SimpleCode(this, setValue, null, 0);
    }

    @Override
    public boolean flagop(int fl) {
        return (fl & STD_CONST) != 0 && field.constValue != null;
    }
}
