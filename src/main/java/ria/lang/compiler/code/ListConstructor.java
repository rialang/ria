package ria.lang.compiler.code;

import ria.lang.compiler.CodeGen;
import ria.lang.compiler.Context;

import java.util.Arrays;
import java.util.List;

public final class ListConstructor extends Code implements CodeGen {
    private Code[] items;
    private List<Object> key;

    public ListConstructor(Code[] items) {
        int i;
        this.items = items;
        for(i = 0; i < items.length; ++i) {
            if(!items[i].flagop(CONST)) {
                return;
            }
        }
        // good, got constant list
        Object[] ak = new Object[items.length + 1];
        ak[0] = "LIST";
        for(i = 0; i < items.length; ++i) {
            ak[i + 1] = items[i].valueKey();
        }
        key = Arrays.asList(ak);
    }

    @Override
    public void gen2(Context context, Code param, int line) {
        for(Code item : items) {
            if(!(item instanceof Range)) {
                context.typeInsn(NEW, "ria/lang/RiaList");
                context.insn(DUP);
            }
            item.gen(context);
        }
        context.insn(ACONST_NULL);
        for(int i = items.length; --i >= 0; ) {
            if(items[i] instanceof Range) {
                context.methodInsn(INVOKESTATIC, "ria/lang/ListRange",
                    "range", "(Ljava/lang/Object;Ljava/lang/Object;"
                        + "Lria/lang/AbstractList;)Lria/lang/AbstractList;");
            } else {
                context.visitInit("ria/lang/RiaList",
                    "(Ljava/lang/Object;Lria/lang/AbstractList;)V");
            }
        }
    }

    @Override
    public void gen(Context context) {
        if(items.length == 0) {
            context.insn(ACONST_NULL);
            return;
        }
        if(key == null) {
            gen2(context, null, 0);
        } else {
            context.constant(key, new SimpleCode(this, null, type, 0));
        }
        context.forceType("ria/lang/AbstractList");
    }

    @Override
    Object valueKey() {
        return key;
    }

    @Override
    public boolean flagop(int fl) {
        return (fl & STD_CONST) != 0 && (key != null || items.length == 0) ||
            (fl & EMPTY_LIST) != 0 && items.length == 0 ||
            (fl & LIST_RANGE) != 0 && items.length != 0
                && items[0] instanceof Range;
    }
}
