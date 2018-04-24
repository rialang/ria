package ria.lang.compiler.code;

import ria.lang.compiler.Binder;
import ria.lang.compiler.CType;
import ria.lang.compiler.Context;

import java.util.Objects;

public final class ArithOpFun extends BinOpRef {
    private String method;
    private int line;

    public ArithOpFun(String fun, String method, CType type,
                      Binder binder, int line) {
        this.type = type;
        this.method = method;
        coreFun = fun;
        this.binder = binder;
        this.line = line;
    }

    public BindRef getRef(int line) {
        return this; // XXX should copy for type?
    }

    @Override
    void binGen(Context context, Code arg1, Code arg2) {
        boolean arg2IsInt = arg2.flagop(INT_NUM);
        if(Objects.equals(method, "and") && arg2IsInt) {
            context.typeInsn(NEW, "ria/lang/IntNum");
            context.insn(DUP);
            arg1.gen(context);
            context.visitLine(line);
            context.typeInsn(CHECKCAST, "ria/lang/RiaNum");
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/RiaNum", "longValue", "()J");
            arg2.genInt(context, line, true);
            context.insn(LAND);
            context.visitInit("ria/lang/IntNum", "(J)V");
        } else {
            arg1.gen(context);
            context.visitLine(line);
            context.typeInsn(CHECKCAST, "ria/lang/RiaNum");
            if(Objects.equals(method, "shl") || Objects.equals(method, "shr")) {
                arg2.genInt(context, line, false);
                if(Objects.equals(method, "shr")) {
                    context.insn(INEG);
                }
                context.methodInsn(INVOKEVIRTUAL, "ria/lang/RiaNum", "shl", "(I)Lria/lang/RiaNum;");
            } else if(arg2IsInt) {
                boolean ii = Objects.equals(method, "intDiv") || Objects.equals(method, "rem");
                arg2.genInt(context, line, !ii);
                context.visitLine(line);
                context.methodInsn(INVOKEVIRTUAL, "ria/lang/RiaNum", method, ii ? "(I)Lria/lang/RiaNum;" : "(J)Lria/lang/RiaNum;");
            } else {
                arg2.gen(context);
                context.visitLine(line);
                context.typeInsn(CHECKCAST, "ria/lang/RiaNum");
                context.methodInsn(INVOKEVIRTUAL, "ria/lang/RiaNum", method, "(Lria/lang/RiaNum;)Lria/lang/RiaNum;");
            }
        }
        context.forceType("ria/lang/RiaNum");
    }
}
