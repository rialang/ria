package ria.lang.compiler.code;

import ria.lang.compiler.Context;
import ria.lang.compiler.JavaType;
import ria.lang.compiler.RiaType;

public final class Throw extends IsNullPtr {
    public Throw(int line) {
        super(RiaType.THROW_TYPE, "throw", line);
    }

    @Override
    void gen(Context context, Code arg, int line) {
        arg.gen(context);
        context.visitLine(line);
        JavaType t = arg.type.deref().javaType;
        context.typeInsn(CHECKCAST,
            t != null ? t.className() : "java/lang/Throwable");
        context.insn(ATHROW);
    }
}


