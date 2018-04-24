package ria.lang.compiler.code;

import ria.lang.compiler.CType;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class Cast extends JavaExpr {
    private boolean convert;

    public Cast(Code code, CType type, boolean convert, int line) {
        super(code, null, null, line);
        this.type = type;
        this.line = line;
        this.convert = convert;
    }

    @Override
    public void gen(Context context) {
        if(convert) {
            convertedArg(context, object, type.deref(), line);
            return;
        }
        object.gen(context);
        if(type.deref().type == RiaType.UNIT) {
            context.insn(POP);
            context.insn(ACONST_NULL);
        }
    }

    @Override
    boolean prepareConst(Context context) {
        return object.prepareConst(context);
    }

    @Override
    public boolean flagop(int fl) {
        return ((fl & CONST) != 0 ? !convert : (fl & PURE) != 0) &&
            object.flagop(fl);
    }
}
