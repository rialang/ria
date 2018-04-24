package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.CType;
import ria.lang.compiler.Context;

public class IsNullPtr extends StaticRef {
    boolean normalIf;
    private String libName;

    public IsNullPtr(CType type, String fun, int line) {
        super(fun, type, true, line);
    }

    @Override
    public Code apply(final Code arg, final CType res, final int line) {
        return new Code() {
            {
                type = res;
            }

            @Override
            public void gen(Context context) {
                IsNullPtr.this.gen(context, arg, line);
            }

            @Override
            void genIf(Context context, Label to, boolean ifTrue) {
                if(normalIf) {
                    super.genIf(context, to, ifTrue);
                } else {
                    IsNullPtr.this.genIf(context, arg, to, ifTrue, line);
                }
            }
        };
    }

    void gen(Context context, Code arg, int line) {
        Label label = new Label();
        genIf(context, arg, label, false, line);
        context.genBoolean(label);
    }

    void genIf(Context context, Code arg, Label to, boolean ifTrue, int line) {
        arg.gen(context);
        context.jumpInsn(ifTrue ? IFNULL : IFNONNULL, to);
    }
}
