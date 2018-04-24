package ria.lang.compiler.code;

import ria.lang.compiler.Binder;
import ria.lang.compiler.CType;
import ria.lang.compiler.CodeGen;
import ria.lang.compiler.CompileException;
import ria.lang.compiler.Context;
import ria.lang.compiler.code.SimpleCode;
import ria.lang.compiler.code.StringConstant;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class RegexFun extends StaticRef implements CodeGen {
    private String impl;
    private String funName;

    public RegexFun(String fun, String impl, CType type, Binder binder, int line) {
        super(fun, type, false, line);
        this.funName = fun;
        this.binder = binder;
        this.impl = impl;
    }

    @Override
    public void gen2(Context context, Code arg, int line) {
        context.typeInsn(NEW, impl);
        context.insn(DUP);
        arg.gen(context);
        context.visitLine(line);
        context.visitInit(impl, "(Ljava/lang/Object;)V");
    }

    @Override
    public Code apply(final Code arg, final CType t, final int line) {
        final Code f = new SimpleCode(this, arg, t, line);
        if(!(arg instanceof StringConstant)) {
            return f;
        }
        try {
            Pattern.compile(((StringConstant)arg).str, Pattern.DOTALL);
        } catch(PatternSyntaxException ex) {
            throw new CompileException(line, 0, "Bad pattern syntax: " + ex.getMessage());
        }
        return new Code() {
            {
                type = t;
            }

            @Override
            public void gen(Context context) {
                context.constant(funName + ":regex:" +
                    ((StringConstant)arg).str, f);
            }
        };
    }
}
