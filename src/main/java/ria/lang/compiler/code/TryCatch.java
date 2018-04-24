package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Binder;
import ria.lang.compiler.CType;
import ria.lang.compiler.Context;
import ria.lang.compiler.code.BindRef;

import java.util.ArrayList;
import java.util.List;

/*
 * Since the stupid JVM discards local stack when catching exceptions,
 * try-catch blocks have to be converted into closures
 * (at least for the generic case).
 */
public final class TryCatch extends CapturingClosure {
    public Code block;
    public Code cleanup;
    private List<Catch> catches = new ArrayList<>();
    private int exVar;

    public void setBlock(Code block) {
        this.type = block.type;
        this.block = block;
    }

    public Catch addCatch(CType ex) {
        Catch c = new Catch();
        c.type = ex;
        catches.add(c);
        return c;
    }

    @Override
    void captureInit(Context context, Capture c, int n) {
        c.localVar = n;
        c.captureGen(context);
    }

    @Override
    public void gen(Context context) {
        int argc = mergeCaptures(context, true);
        StringBuilder sigb = new StringBuilder("(");
        for(Capture c = captures; c != null; c = c.next) {
            sigb.append(c.captureType());
        }
        sigb.append(")Ljava/lang/Object;");
        String sig = sigb.toString();
        String name = context.methodName(null);
        context.methodInsn(INVOKESTATIC, context.className, name, sig);
        Context mc = context.newMethod(ACC_PRIVATE | ACC_STATIC, name, sig);
        mc.localVarCount = argc;

        Label codeStart = new Label(), codeEnd = new Label();
        Label cleanupStart = cleanup == null ? null : new Label();
        Label cleanupEntry = cleanup == null ? null : new Label();
        genClosureInit(mc);
        int retVar = -1;
        if(cleanupStart != null) {
            retVar = mc.localVarCount++;
            mc.insn(ACONST_NULL);
            mc.varInsn(ASTORE, retVar); // silence the JVM verifier...
        }
        mc.visitLabel(codeStart);
        block.gen(mc);
        mc.visitLabel(codeEnd);
        exVar = mc.localVarCount++;
        if(cleanupStart != null) {
            Label goThrow = new Label();
            mc.visitLabel(cleanupEntry);
            mc.varInsn(ASTORE, retVar);
            mc.insn(ACONST_NULL);
            mc.visitLabel(cleanupStart);
            mc.varInsn(ASTORE, exVar);
            cleanup.gen(mc);
            mc.insn(POP); // cleanup's null
            mc.load(exVar).jumpInsn(IFNONNULL, goThrow);
            mc.load(retVar).insn(ARETURN);
            mc.visitLabel(goThrow);
            mc.load(exVar).insn(ATHROW);
        } else {
            mc.insn(ARETURN);
        }
        for(Object catche : catches) {
            Catch c = (Catch)catche;
            Label catchStart = new Label();
            mc.tryCatchBlock(codeStart, codeEnd, catchStart,
                c.type.javaType.className());
            Label catchEnd = null;
            if(cleanupStart != null) {
                catchEnd = new Label();
                mc.tryCatchBlock(catchStart, catchEnd, cleanupStart, null);
            }
            mc.visitLabel(catchStart);
            mc.varInsn(ASTORE, exVar);
            c.handler.gen(mc);
            if(catchEnd != null) {
                mc.visitLabel(catchEnd);
                mc.jumpInsn(GOTO, cleanupEntry);
            } else {
                mc.insn(ARETURN);
            }
        }
        if(cleanupStart != null) {
            mc.tryCatchBlock(codeStart, codeEnd, cleanupStart, null);
        }
        mc.closeMethod();
    }

    public final class Catch extends BindRef implements Binder {
        public Code handler;

        @Override
        public BindRef getRef(int line) {
            return this;
        }

        @Override
        public void gen(Context context) {
            context.load(exVar);
        }
    }
}
