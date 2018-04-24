package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.CType;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;
import ria.lang.compiler.code.CaseExpr;
import ria.lang.compiler.code.Core2;
import ria.lang.compiler.code.LoadVar;

public class For extends Core2 {
    public For(int line) {
        super("for", RiaType.FOR_TYPE, line);
    }

    protected For(String coreFun, CType type, int line) {
        super(coreFun, type, line);
    }

    @Override
    void genApply2(Context context, Code list, Code fun, int line) {
        Function f;
        LoadVar arg = new LoadVar();
        CType t;
        if(!list.flagop(LIST_RANGE) && fun instanceof Function &&
            ((f = (Function)fun).body instanceof CaseExpr ||
                (t = list.type.deref()).type == RiaType.MAP &&
                    t.param[1].deref().type == RiaType.NONE) &&
            f.uncapture(arg)) {
            Label retry = new Label(), end = new Label();
            list.gen(context);
            context.visitLine(line);
            context.typeInsn(CHECKCAST, "ria/lang/AbstractList");
            context.insn(DUP);
            context.jumpInsn(IFNULL, end);
            context.insn(DUP);
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractList", "isEmpty", "()Z");
            context.jumpInsn(IFNE, end);
            // start of loop
            context.visitLabel(retry);
            context.insn(DUP);
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractIterator", "first", "()Ljava/lang/Object;");
            // invoke body block
            context.varInsn(ASTORE, arg.var = context.localVarCount++);
            ++context.tainted; // disable argument-nulling - we're in cycle
            // new closure has to be created on each cycle
            // as closure vars could be captured
            f.genClosureInit(context);
            f.body.gen(context);
            --context.tainted;
            context.visitLine(line);
            context.insn(POP); // ignore return value
            // next
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractIterator", "next", "()Lria/lang/AbstractIterator;");
            context.insn(DUP);
            context.jumpInsn(IFNONNULL, retry);
            context.visitLabel(end);
        } else {
            Label nop = new Label(), end = new Label();
            list.gen(context);
            fun.gen(context);
            context.visitLine(line);
            context.insn(SWAP);
            context.typeInsn(CHECKCAST, "ria/lang/AbstractList");
            context.insn(DUP_X1);
            context.jumpInsn(IFNULL, nop);
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractList", "forEach", "(Ljava/lang/Object;)V");
            context.jumpInsn(GOTO, end);
            context.visitLabel(nop);
            context.insn(POP2);
            context.visitLabel(end);
            context.insn(ACONST_NULL);
        }
    }
}
