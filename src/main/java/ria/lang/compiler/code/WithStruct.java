package ria.lang.compiler.code;

import ria.lang.compiler.CType;
import ria.lang.compiler.Context;

import java.util.Arrays;
import java.util.Map;

public final class WithStruct extends Code {
    private Code src;
    private Code override;
    private String[] names;

    public WithStruct(CType type, Code src, Code override, String[] names) {
        this.type = type;
        this.src = src;
        this.override = override;
        this.names = names;
        this.polymorph = src.polymorph && override.polymorph;
    }

    @Override
    public void gen(Context context) {
        Map<String, CType> srcFields = src.type.deref().allowedMembers;
        if(srcFields != null && override instanceof StructConstructor) {
            ((StructConstructor)override).genWith(context, src, srcFields);
            return;
        }

        context.typeInsn(NEW, "ria/lang/WithStruct");
        context.insn(DUP);
        src.gen(context);
        context.typeInsn(CHECKCAST, "ria/lang/Struct");
        override.gen(context);
        context.typeInsn(CHECKCAST, "ria/lang/Struct");
        Arrays.sort(names);
        String[] a = new String[names.length + 1];
        System.arraycopy(names, 0, a, 1, names.length);
        context.constants.stringArray(context, a);
        context.intConst(srcFields != null ? 1 : 0);
        context.visitInit("ria/lang/WithStruct", "(Lria/lang/Struct;Lria/lang/Struct;[Ljava/lang/String;Z)V");
        context.forceType("ria/lang/Struct");
    }
}
