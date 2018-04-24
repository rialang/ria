package ria.lang.compiler;

import org.objectweb.asm.Opcodes;
import ria.lang.compiler.code.BindRef;
import ria.lang.compiler.code.Code;
import ria.lang.compiler.code.SimpleCode;

public final class EvalBind implements Binder, CaptureWrapper, Opcodes, CodeGen {
    private RiaEval.Binding bind;

    public EvalBind(RiaEval.Binding bind) {
        this.bind = bind;
    }

    @Override
    public void gen2(Context context, Code value, int line) {
        genPreGet(context);
        genSet(context, value);
        context.insn(ACONST_NULL);
    }

    @Override
    public BindRef getRef(int line) {
        return new BindRef() {
            {
                type = bind.type;
                binder = EvalBind.this;
                polymorph = !bind.mutable && bind.polymorph;
            }

            @Override
            public void gen(Context context) {
                genPreGet(context);
                genGet(context);
            }

            @Override
            public Code assign(final Code value) {
                return bind.mutable ?
                    new SimpleCode(EvalBind.this, value, null, 0) : null;
            }

            @Override
            public boolean flagop(int fl) {
                return (fl & ASSIGN) != 0 && bind.mutable;
            }

            @Override
            public CaptureWrapper capture() {
                return EvalBind.this;
            }
        };
    }

    @Override
    public void genPreGet(Context context) {
        context.intConst(bind.bindId);
        context.methodInsn(INVOKESTATIC, "ria/lang/compiler/RiaEval", "getBind", "(I)[Ljava/lang/Object;");
    }

    @Override
    public void genGet(Context context) {
        context.intConst(bind.index);
        context.insn(AALOAD);
    }

    @Override
    public void genSet(Context context, Code value) {
        context.intConst(bind.index);
        value.gen(context);
        context.insn(AASTORE);
    }

    @Override
    public Object captureIdentity() {
        return this;
    }

    @Override
    public String captureType() {
        return "[Ljava/lang/Object;";
    }
}
