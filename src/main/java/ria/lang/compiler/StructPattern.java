package ria.lang.compiler;

import org.objectweb.asm.Label;

public final class StructPattern extends CasePattern {
    private String[] names;
    private CasePattern[] patterns;

    StructPattern(String[] names, CasePattern[] patterns) {
        this.names = names;
        this.patterns = patterns;
    }

    @Override
    public int preparePattern(Context context) {
        return 1;
    }

    @Override
    public void tryMatch(Context context, Label onFail, boolean preserve) {
        boolean dropped = false;
        Label failed = preserve ? onFail : new Label();
        for (int i = 0; i < names.length; ++i) {
            if (patterns[i] == ANY_PATTERN) {
                continue;
            }
            if (preserve || i != names.length - 1) {
                context.insn(DUP);
            } else {
                dropped = true;
            }
            context.ldcInsn(names[i]);
            context.methodInsn(INVOKEINTERFACE, "ria/lang/Struct", "get", "(Ljava/lang/String;)Ljava/lang/Object;");
            patterns[i].preparePattern(context);
            patterns[i].tryMatch(context, i < names.length - 1 ? failed : onFail, false);
        }
        if (!preserve && names.length > 1) {
            Label ok = new Label();
            context.jumpInsn(GOTO, ok);
            context.visitLabel(failed);
            context.insn(POP);
            context.jumpInsn(GOTO, onFail);
            context.visitLabel(ok);
            if (!dropped) {
                context.insn(POP);
            }
        }
    }
}
