package ria.lang.compiler.code;

import ria.lang.compiler.Context;

public final class MapConstructor extends Code {
    private Code[] keyItems;
    private Code[] items;

    public MapConstructor(Code[] keyItems, Code[] items) {
        this.keyItems = keyItems;
        this.items = items;
    }

    @Override
    public void gen(Context context) {
        context.typeInsn(NEW, "ria/lang/Hash");
        context.insn(DUP);
        if(keyItems.length > 16) {
            context.intConst(keyItems.length);
            context.visitInit("ria/lang/Hash", "(I)V");
        } else {
            context.visitInit("ria/lang/Hash", "()V");
        }
        for(int i = 0; i < keyItems.length; ++i) {
            context.insn(DUP);
            keyItems[i].gen(context);
            items[i].gen(context);
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/Hash", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
            context.insn(POP);
        }
    }

    @Override
    public boolean flagop(int fl) {
        return (fl & EMPTY_LIST) != 0 && keyItems.length == 0;
    }
}
