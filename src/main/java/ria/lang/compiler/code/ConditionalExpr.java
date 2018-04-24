package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.CType;
import ria.lang.compiler.Context;

public final class ConditionalExpr extends Code {
    private Code[][] choices;

    public ConditionalExpr(CType type, Code[][] choices, boolean poly) {
        this.type = type;
        this.choices = choices;
        this.polymorph = poly;
    }

    @Override
    public void gen(Context context) {
        Label end = new Label();
        for(int i = 0, last = choices.length - 1; i <= last; ++i) {
            Label jmpNext = i < last ? new Label() : end;
            if(choices[i].length == 2) {
                choices[i][1].genIf(context, jmpNext, false); // condition
                choices[i][0].gen(context); // body
                context.jumpInsn(GOTO, end);
            } else {
                choices[i][0].gen(context);
            }
            context.visitLabel(jmpNext);
        }
        context.insn(-1); // reset type
    }

    @Override
    void genIf(Context context, Label to, boolean ifTrue) {
        Label end = new Label();
        for(int i = 0, last = choices.length - 1; i <= last; ++i) {
            Label jmpNext = i < last ? new Label() : end;
            if(choices[i].length == 2) {
                choices[i][1].genIf(context, jmpNext, false); // condition
                choices[i][0].genIf(context, to, ifTrue); // body
                context.jumpInsn(GOTO, end);
            } else {
                choices[i][0].genIf(context, to, ifTrue);
            }
            context.visitLabel(jmpNext);
        }
    }

    @Override
    public void markTail() {
        for(int i = choices.length; --i >= 0; ) {
            choices[i][0].markTail();
        }
    }
}
