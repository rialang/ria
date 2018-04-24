package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;
import ria.lang.compiler.code.Apply;
import ria.lang.compiler.code.Core2;

public final class Latch extends Core2 {
    public Latch(int line) {
        super("latch", RiaType.SYNCHRONIZED_TYPE, line);
    }

    @Override
    void genApply2(Context context, Code monitor, Code block, int line) {
        monitor.gen(context);
        int monitorVar = context.localVarCount++;
        context.visitLine(line);
        context.insn(DUP);
        context.varInsn(ASTORE, monitorVar);
        context.insn(MONITORENTER);

        Label startBlock = new Label(), endBlock = new Label();
        context.visitLabel(startBlock);
        new Apply(type, block, new UnitConstant(null), line).gen(context);
        context.visitLine(line);
        context.load(monitorVar).insn(MONITOREXIT);
        context.visitLabel(endBlock);
        Label end = new Label();
        context.jumpInsn(GOTO, end);

        Label startCleanup = new Label(), endCleanup = new Label();
        context.tryCatchBlock(startBlock, endBlock, startCleanup, null);
        // I have no idea what this second catch is supposed
        // to be doing. javac generates it, so it has to be good :-)
        context.tryCatchBlock(startCleanup, endCleanup, startCleanup, null);

        int exceptionVar = context.localVarCount++;
        context.visitLabel(startCleanup);
        context.varInsn(ASTORE, exceptionVar);
        context.load(monitorVar).insn(MONITOREXIT);
        context.visitLabel(endCleanup);
        context.load(exceptionVar).insn(ATHROW);
        context.visitLabel(end);
    }
}
