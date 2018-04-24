package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.CType;
import ria.lang.compiler.CodeGen;
import ria.lang.compiler.CompileException;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;
import ria.lang.compiler.code.BinOpRef;
import ria.lang.compiler.code.SimpleCode;
import ria.lang.compiler.code.StringConstant;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class MatchOpFun extends BinOpRef implements CodeGen {
    private int line;
    private boolean yes;

    public MatchOpFun(int line, boolean yes) {
        type = RiaType.STR2_PRED_TYPE;
        coreFun = mangle(yes ? "=~" : "!~");
        this.line = line;
        this.yes = yes;
    }

    @Override
    void binGen(Context context, Code arg1, final Code arg2) {
        apply2nd(arg2, RiaType.STR2_PRED_TYPE, line).gen(context);
        context.visitApply(arg1, line);
    }

    @Override
    public void gen2(Context context, Code arg2, int line) {
        context.typeInsn(NEW, "ria/lang/Match");
        context.insn(DUP);
        arg2.gen(context);
        context.intConst(yes ? 1 : 0);
        context.visitLine(line);
        context.visitInit("ria/lang/Match", "(Ljava/lang/Object;Z)V");
    }

    @Override
    public Code apply2nd(final Code arg2, final CType t, final int line) {
        if(line == 0) {
            throw new NullPointerException();
        }
        final Code matcher = new SimpleCode(this, arg2, t, line);
        if(!(arg2 instanceof StringConstant)) {
            return matcher;
        }
        try {
            Pattern.compile(((StringConstant)arg2).str, Pattern.DOTALL);
        } catch(PatternSyntaxException ex) {
            throw new CompileException(line, 0, "Bad pattern syntax: " + ex.getMessage());
        }
        return new Code() {
            {
                type = t;
            }

            @Override
            public void gen(Context context) {
                context.constant((yes ? "MATCH-FUN:" : "MATCH!FUN:").concat(((StringConstant)arg2).str), matcher);
            }
        };
    }

    @Override
    void binGenIf(Context context, Code arg1, Code arg2, Label to, boolean ifTrue) {
        binGen(context, arg1, arg2);
        context.fieldInsn(GETSTATIC, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;");
        context.jumpInsn(ifTrue ? IF_ACMPEQ : IF_ACMPNE, to);
    }
}
