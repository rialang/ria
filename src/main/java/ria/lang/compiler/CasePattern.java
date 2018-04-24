package ria.lang.compiler;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

public abstract class CasePattern implements Opcodes {
    static final CasePattern ANY_PATTERN = new CasePattern() {
        @Override
        public void tryMatch(Context context, Label onFail, boolean preserve) {
            if (!preserve) {
                context.insn(POP);
            }
        }

        @Override
        boolean irrefutable() {
            return true;
        }
    };

    public int preparePattern(Context context) {
        return 1;
    }

    public abstract void tryMatch(Context context, Label onFail, boolean preserve);

    boolean irrefutable() {
        return false;
    }
}
