package ria.lang.compiler.code;

import ria.lang.compiler.CType;
import ria.lang.compiler.CodeGen;
import ria.lang.compiler.Context;

import java.util.Arrays;

public final class VariantConstructor extends Code implements CodeGen {
    public String name;

    public VariantConstructor(CType type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public void gen2(Context context, Code param, int line) {
        context.typeInsn(NEW, "ria/lang/TagConstructor");
        context.insn(DUP);
        context.ldcInsn(name);
        context.visitInit("ria/lang/TagConstructor", "(Ljava/lang/String;)V");
    }

    @Override
    public void gen(Context context) {
        context.constant("TAG:".concat(name), new SimpleCode(this, null, type, 0));
    }

    @Override
    public Code apply(final Code arg, CType res, int line) {
        class Tag extends Code implements CodeGen {
            private Object key;

            @Override
            public void gen2(Context ctx, Code param, int line_) {
                ctx.typeInsn(NEW, "ria/lang/Tag");
                ctx.insn(DUP);
                arg.gen(ctx);
                ctx.ldcInsn(name);
                ctx.visitInit("ria/lang/Tag", "(Ljava/lang/Object;Ljava/lang/String;)V");
            }

            @Override
            public void gen(Context ctx) {
                if(key != null) {
                    ctx.constant(key, new SimpleCode(this, null, type, 0));
                } else {
                    gen2(ctx, null, 0);
                }
            }

            @Override
            public boolean flagop(int fl) {
                return (fl & STD_CONST) != 0 && key != null;
            }

            @Override
            Object valueKey() {
                return key == null ? this : key;
            }
        }

        Tag tag = new Tag();
        tag.type = res;
        tag.polymorph = arg.polymorph;
        if(arg.flagop(CONST)) {
            Object[] key = {"TAG", name, arg.valueKey()};
            tag.key = Arrays.asList(key);
        }
        return tag;
    }
}

