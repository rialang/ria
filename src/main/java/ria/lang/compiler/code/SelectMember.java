package ria.lang.compiler.code;

import ria.lang.compiler.CType;
import ria.lang.compiler.CodeGen;
import ria.lang.compiler.Context;
import ria.lang.compiler.code.BindRef;
import ria.lang.compiler.code.SimpleCode;

public abstract class SelectMember extends BindRef implements CodeGen {
    public Code st;
    String name;
    int line;
    private boolean assigned = false;

    public SelectMember(CType type, Code st, String name, int line,
                        boolean polymorph) {
        this.type = type;
        this.polymorph = polymorph;
        this.st = st;
        this.name = name;
        this.line = line;
    }

    @Override
    public void gen(Context context) {
        st.gen(context);
        context.visitLine(line);
        context.ldcInsn(name);
        context.methodInsn(INVOKEINTERFACE, "ria/lang/Struct", "get", "(Ljava/lang/String;)Ljava/lang/Object;");
    }

    @Override
    public void gen2(Context context, Code setValue, int __) {
        st.gen(context);
        context.visitLine(line);
        context.ldcInsn(name);
        setValue.gen(context);
        context.visitLine(line);
        context.methodInsn(INVOKEINTERFACE, "ria/lang/Struct", "set", "(Ljava/lang/String;Ljava/lang/Object;)V");
        context.insn(ACONST_NULL);
    }

    @Override
    public Code assign(final Code setValue) {
        if(!assigned && !mayAssign()) {
            return null;
        }
        assigned = true;
        return new SimpleCode(this, setValue, null, 0);
    }

    public abstract boolean mayAssign();
}
