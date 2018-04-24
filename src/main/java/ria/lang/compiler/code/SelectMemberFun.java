package ria.lang.compiler.code;

import ria.lang.compiler.CType;
import ria.lang.compiler.CodeGen;
import ria.lang.compiler.Context;

public final class SelectMemberFun extends Code implements CodeGen {
    private String[] names;

    public SelectMemberFun(CType type, String[] names) {
        this.type = type;
        this.names = names;
        this.polymorph = true;
    }

    @Override
    public void gen2(Context context, Code param, int line) {
        for(int i = 1; i < names.length; ++i) {
            context.typeInsn(NEW, "ria/lang/Compose");
            context.insn(DUP);
        }
        for(int i = names.length; --i >= 0; ) {
            context.typeInsn(NEW, "ria/lang/Selector");
            context.insn(DUP);
            context.ldcInsn(names[i]);
            context.visitInit("ria/lang/Selector", "(Ljava/lang/String;)V");
            if(i + 1 != names.length) {
                context.visitInit("ria/lang/Compose", "(Ljava/lang/Object;Ljava/lang/Object;)V");
            }
        }
    }

    @Override
    public void gen(Context context) {
        StringBuilder buf = new StringBuilder("SELECTMEMBER");
        for(String name : names) {
            buf.append(':');
            buf.append(name);
        }
        context.constant(buf.toString(), new SimpleCode(this, null, type, 0));
    }
}
