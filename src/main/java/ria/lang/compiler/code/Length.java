package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.CType;
import ria.lang.compiler.Context;
import ria.lang.compiler.JavaType;
import ria.lang.compiler.RiaType;

public final class Length extends StaticRef {
    public Length() {
        super("length", RiaType.MAP_TO_NUM, true, 0);
    }

    void genLong(Context context, Code arg, int line, boolean toint) {
        if(arg instanceof Cast) {
            Code obj = ((Cast)arg).object;
            CType t = obj.type.deref();
            if(t.type == RiaType.JAVA_ARRAY) {
                obj.gen(context);
                context.typeInsn(CHECKCAST, JavaType.descriptionOf(t));
                context.visitLine(line);
                context.insn(ARRAYLENGTH);
                if(!toint) {
                    context.insn(I2L);
                }
                return;
            }
        }
        Label nonnull = new Label(), end = new Label();
        arg.gen(context);
        context.visitLine(line);
        // arrays can't be null, other can
        if(arg.type.deref().param[1].deref() != RiaType.NUM_TYPE) {
            context.insn(DUP);
            context.jumpInsn(IFNONNULL, nonnull);
            context.insn(POP);
            if(toint) {
                context.intConst(0);
            } else {
                context.ldcInsn(0L);
            }
            context.jumpInsn(GOTO, end);
            context.visitLabel(nonnull);
        }
        context.methodInsn(INVOKEINTERFACE, "ria/lang/Collection", "length", "()J");
        if(toint) {
            context.insn(L2I);
        }
        context.visitLabel(end);
    }

    @Override
    public Code apply(final Code arg, final CType res, final int line) {
        return new Code() {
            {
                type = res;
            }

            @Override
            public void gen(Context context) {
                context.typeInsn(NEW, "ria/lang/IntNum");
                context.insn(DUP);
                genLong(context, arg, line, false);
                context.visitInit("ria/lang/IntNum", "(J)V");
                context.forceType("ria/lang/RiaNum");
            }

            @Override
            void genInt(Context context, int line_, boolean longValue) {
                genLong(context, arg, line, !longValue);
            }

            @Override
            public boolean flagop(int fl) {
                return (fl & INT_NUM) != 0;
            }
        };
    }
}
