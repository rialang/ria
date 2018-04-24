package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.CasePattern;
import ria.lang.compiler.Context;

import java.util.ArrayList;
import java.util.List;

public final class CaseExpr extends Code {
    private int totalParams;
    private Code caseValue;
    private List<Choice> choices = new ArrayList<>();
    public int paramStart;
    public int paramCount;

    public CaseExpr(Code caseValue) {
        this.caseValue = caseValue;
    }

    private static final class Choice {
        CasePattern pattern;
        Code expr;
    }

    public void resetParams() {
        if (totalParams < paramCount) {
            totalParams = paramCount;
        }
        paramCount = 0;
    }

    public void addChoice(CasePattern pattern, Code code) {
        Choice c = new Choice();
        c.pattern = pattern;
        c.expr = code;
        choices.add(c);
    }

    @Override
    public void gen(Context context) {
        caseValue.gen(context);
        paramStart = context.localVarCount;
        context.localVarCount += totalParams;
        Label next = null, end = new Label();
        CasePattern lastPattern = (choices.get(0)).pattern;
        int patternStack = lastPattern.preparePattern(context);

        for (int last = choices.size() - 1, i = 0; i <= last; ++i) {
            Choice c = choices.get(i);
            if (lastPattern.getClass() != c.pattern.getClass()) {
                context.popn(patternStack - 1);
                patternStack = c.pattern.preparePattern(context);
            }
            lastPattern = c.pattern;
            next = new Label();
            c.pattern.tryMatch(context, next, true);
            context.popn(patternStack);
            c.expr.gen(context);
            context.jumpInsn(GOTO, end);
            context.visitLabel(next);
        }
        context.visitLabel(next);
        context.popn(patternStack - 1);
        context.methodInsn(INVOKESTATIC, "ria/lang/Core",
                       "badMatch", "(Ljava/lang/Object;)Ljava/lang/Object;");
        context.visitLabel(end);
    }

    @Override
    public void markTail() {
        for (int i = choices.size(); --i >= 0;) {
            (choices.get(i)).expr.markTail();
        }
    }
}
