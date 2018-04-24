package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.CType;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class NotOp extends StaticRef {
    public NotOp(int line) {
        super("not", RiaType.BOOL_TO_BOOL, false, line);
    }

    @Override
    public Code apply(final Code arg, CType res, int line) {
        return new Code() {
            {
                type = RiaType.BOOL_TYPE;
            }

            @Override
            void genIf(Context context, Label to, boolean ifTrue) {
                arg.genIf(context, to, !ifTrue);
            }

            @Override
            public void gen(Context context) {
                Label label = new Label();
                arg.genIf(context, label, true);
                context.genBoolean(label);
            }
        };
    }
}
