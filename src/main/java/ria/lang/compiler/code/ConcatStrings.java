package ria.lang.compiler.code;

import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class ConcatStrings extends Code {
    Code[] param;

    public ConcatStrings(Code[] param) {
        type = RiaType.STR_TYPE;
        this.param = param;
    }

    @Override
    public void gen(Context context) {
        boolean arr = false;
        if(param.length > 2) {
            arr = true;
            context.intConst(param.length);
            context.typeInsn(ANEWARRAY, "java/lang/String");
        }
        for(int i = 0; i < param.length; ++i) {
            if(arr) {
                context.insn(DUP);
                context.intConst(i);
            }
            param[i].gen(context);
            boolean valueOf = param[i].type.deref().type != RiaType.STR;
            if(valueOf) {
                context.methodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
            }
            if(arr) {
                context.insn(AASTORE);
            } else if(!valueOf) {
                context.typeInsn(CHECKCAST, "java/lang/String");
            }
        }
        if(arr) {
            context.methodInsn(INVOKESTATIC, "ria/lang/Core", "concat", "([Ljava/lang/String;)Ljava/lang/String;");
        } else if(param.length == 2) {
            context.methodInsn(INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;");
        }
        context.forceType("java/lang/String");
    }
}
