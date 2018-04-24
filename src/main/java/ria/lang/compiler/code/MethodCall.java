package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Context;
import ria.lang.compiler.JavaType;
import ria.lang.compiler.RiaType;
import ria.lang.compiler.code.JavaExpr;

import java.util.Objects;

public final class MethodCall extends JavaExpr {
    private JavaType classType;
    private boolean useAccessor, invokeSuper;

    public MethodCall(Code object, JavaType.Method method, Code[] args, int line) {
        super(object, method, args, line);
        type = method.convertedReturnType();
    }

    @Override
    void visitInvoke(Context context, int invokeInsn) {
        String descr = method.descr(null);
        String name = method.name;
        if(useAccessor) {
            if(invokeInsn == INVOKEINTERFACE) {
                invokeInsn = INVOKEVIRTUAL;
            }
            name = classType.implementation.getAccessor(method, descr, invokeSuper);
        }
        context.methodInsn(invokeInsn, classType.className(), name, descr);
    }

    private void _gen(Context context) {
        classType = method.classType.javaType;
        int ins = object == null ? INVOKESTATIC : classType.isInterface()
            ? INVOKEINTERFACE : INVOKEVIRTUAL;
        if(object != null) {
            object.gen(context);
            if(ins != INVOKEINTERFACE) {
                classType = object.type.deref().javaType;
            }
        }
        if(classType.implementation != null) {
            JavaType ct = classType.implementation.classType.deref().javaType;
            invokeSuper = classType != ct;
            // TODO: not checking for package access. check this shouldn't matter.
            useAccessor = (invokeSuper || (method.access & ACC_PROTECTED) != 0)
                && !object.flagop(DIRECT_THIS);
            if(useAccessor) {
                classType = ct;
            } else if(ins == INVOKEVIRTUAL && invokeSuper) {
                ins = INVOKESPECIAL;
            }
        }
        if(object != null && (ins != INVOKEINTERFACE)) {
            context.typeInsn(CHECKCAST, classType.className());
        }
        genCall(context, null, ins);
    }

    @Override
    public void gen(Context context) {
        _gen(context);
        if(method.returnType.type == RiaType.UNIT) {
            context.insn(ACONST_NULL);
        } else {
            convertValue(context, method.returnType);
        }
    }

    @Override
    void genIf(Context context, Label to, boolean ifTrue) {
        if(method.returnType.javaType != null &&
            Objects.equals(method.returnType.javaType.description, "Z")) {
            _gen(context);
            context.jumpInsn(ifTrue ? IFNE : IFEQ, to);
        } else {
            super.genIf(context, to, ifTrue);
        }
    }
}
