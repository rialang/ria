package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.CType;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;
import ria.lang.compiler.code.StringConstant;

public final class CompareFun extends BoolBinOp {
    public static final int COND_EQ = 0;
    public static final int COND_NOT = 1;
    public static final int COND_LT = 2;
    public static final int COND_GT = 4;
    public static final int COND_LE = COND_NOT | COND_GT;
    public static final int COND_GE = COND_NOT | COND_LT;
    static final int[] OPS = {IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE};
    static final int[] ROP = {IFEQ, IFNE, IFGT, IFLE, IFLT, IFGE};
    public int op;
    public int line;

    @Override
    void binGenIf(Context context, Code arg1, Code arg2, Label to, boolean ifTrue) {
        CType t = arg1.type.deref();
        int op = this.op;
        boolean eq = (op & (COND_LT | COND_GT)) == 0;
        if(!ifTrue) {
            op ^= COND_NOT;
        }
        Label nojmp = null;
        if(t.type == RiaType.VAR || t.type == RiaType.MAP &&
            t.param[2] == RiaType.LIST_TYPE &&
            t.param[1].type != RiaType.NUM) {
            Label notnull = new Label();
            nojmp = new Label();
            arg2.gen(context);
            arg1.gen(context); // 2-1
            context.visitLine(line);
            context.insn(DUP); // 2-1-1
            context.jumpInsn(IFNONNULL, notnull); // 2-1
            // reach here, when 1 was null
            if(op == COND_GT || op == COND_LE ||
                arg2.flagop(EMPTY_LIST) && (op == COND_EQ || op == COND_NOT)) {
                // null is never greater and always less or equal
                context.insn(POP2);
                context.jumpInsn(GOTO, op == COND_LE || op == COND_EQ ? to : nojmp);
            } else {
                context.insn(POP); // 2
                context.jumpInsn(op == COND_EQ || op == COND_GE ? IFNULL : IFNONNULL, to);
                context.jumpInsn(GOTO, nojmp);
            }
            context.visitLabel(notnull);
            context.insn(SWAP); // 1-2
        } else if(arg2 instanceof StringConstant &&
            ((StringConstant)arg2).str.length() == 0 && (op & COND_LT) == 0) {
            arg1.gen(context);
            context.visitLine(line);
            context.typeInsn(CHECKCAST, "java/lang/String");
            context.methodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I");
            context.jumpInsn((op & COND_NOT) == (op >>> 2) ? IFEQ : IFNE, to);
            return;
        } else {
            arg1.gen(context);
            context.visitLine(line);
            if(arg2.flagop(INT_NUM)) {
                context.typeInsn(CHECKCAST, "ria/lang/RiaNum");
                arg2.genInt(context, line, true);
                context.visitLine(line);
                context.methodInsn(INVOKEVIRTUAL, "ria/lang/RiaNum", "rCompare", "(J)I");
                context.jumpInsn(ROP[op], to);
                return;
            }
            arg2.gen(context);
            context.visitLine(line);
        }
        if(eq) {
            op ^= COND_NOT;
            context.methodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z");
        } else {
            context.methodInsn(INVOKEINTERFACE, "java/lang/Comparable", "compareTo", "(Ljava/lang/Object;)I");
        }
        context.jumpInsn(OPS[op], to);
        if(nojmp != null) {
            context.visitLabel(nojmp);
        }
    }
}
