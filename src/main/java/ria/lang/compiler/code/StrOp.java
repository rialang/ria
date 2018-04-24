package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Binder;
import ria.lang.compiler.CType;
import ria.lang.compiler.Context;
import ria.lang.compiler.JavaTypeReader;
import ria.lang.compiler.RiaType;
import ria.lang.compiler.code.Apply;
import ria.lang.compiler.code.BindRef;
import ria.lang.compiler.code.JavaExpr;

import java.util.ArrayList;
import java.util.List;

public final class StrOp extends StaticRef implements Binder {
    final static Code NOP_CODE = new Code() {
        @Override
        public void gen(Context context) {
        }
    };

    String method;
    String sig;
    CType argTypes[];

    public StrOp(String fun, String method, String sig, CType type) {
        super(Code.mangle(fun), type, false, 0);
        this.method = method;
        this.sig = sig;
        binder = this;
        argTypes = JavaTypeReader.parseSig1(1, sig);
    }

    @Override
    public BindRef getRef(int line) {
        return this;
    }

    @Override
    public Code apply(final Code arg, final CType res, final int line) {
        return new StrApply(arg, res, null, line);
    }

    @Override
    public boolean flagop(int fl) {
        return (fl & STD_CONST) != 0;
    }

    final class StrApply extends Apply {
        StrApply prev;

        StrApply(Code arg, CType type, StrApply prev, int line) {
            super(type, NOP_CODE, arg, line);
            this.prev = prev;
        }

        @Override
        public Code apply(Code arg, CType res, int line) {
            return new StrApply(arg, res, this, line);
        }

        void genApply(Context context) {
            super.gen(context);
        }

        @Override
        public void gen(Context context) {
            genIf(context, null, false);
        }

        @Override
        void genIf(Context context, Label to, boolean ifTrue) {
            List<StrApply> argv = new ArrayList<>();
            for(StrApply a = this; a != null; a = a.prev) {
                argv.add(a);
            }
            if(argv.size() != argTypes.length) {
                StrOp.this.gen(context);
                for(int i = argv.size(); --i >= 0; ) {
                    (argv.get(i)).genApply(context);
                }
                return;
            }
            (argv.get(argv.size() - 1)).arg.gen(context);
            context.visitLine(line);
            context.typeInsn(CHECKCAST, "java/lang/String");
            for(int i = 0, last = argv.size() - 2; i <= last; ++i) {
                StrApply a = argv.get(last - i);
                if(a.arg.type.deref().type == RiaType.STR) {
                    a.arg.gen(context);
                    context.typeInsn(CHECKCAST, "java/lang/String");
                } else {
                    JavaExpr.convertedArg(context, a.arg, argTypes[i], a.line);
                }
            }
            context.visitLine(line);
            context.methodInsn(INVOKEVIRTUAL, "java/lang/String",
                method, sig);
            if(to != null) { // really genIf
                context.jumpInsn(ifTrue ? IFNE : IFEQ, to);
            } else if(type.deref().type == RiaType.STR) {
                context.forceType("java/lang/String;");
            } else {
                JavaExpr.convertValue(context, argTypes[argTypes.length - 1]);
            }
        }
    }
}
