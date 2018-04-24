package ria.lang.compiler.code;

import ria.lang.compiler.CType;
import ria.lang.compiler.CodeGen;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class KeyRefExpr extends Code implements CodeGen {
    int line;
    private Code val;
    private Code key;

    public KeyRefExpr(CType type, Code val, Code key, int line) {
        this.type = type;
        this.val = val;
        this.key = key;
        this.line = line;
    }

    @Override
    public void gen(Context context) {
        val.gen(context);
        if(val.type.deref().param[2] == RiaType.LIST_TYPE) {
            context.visitLine(line);
            context.typeInsn(CHECKCAST, "ria/lang/MutableList");
            key.genInt(context, line, false);
            context.visitLine(line);
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/MutableList", "get", "(I)Ljava/lang/Object;");
            return;
        }
        key.gen(context);
        context.visitLine(line);
        context.methodInsn(INVOKEINTERFACE, "ria/lang/ByKey", "vget", "(Ljava/lang/Object;)Ljava/lang/Object;");
    }

    @Override
    public void gen2(Context context, Code setValue, int __) {
        val.gen(context);
        key.gen(context);
        setValue.gen(context);
        context.visitLine(line);
        context.methodInsn(INVOKEINTERFACE, "ria/lang/ByKey",
            "put", "(Ljava/lang/Object;Ljava/lang/Object;)" +
                "Ljava/lang/Object;");
    }

    @Override
    public Code assign(final Code setValue) {
        return new SimpleCode(this, setValue, null, 0);
    }
}
