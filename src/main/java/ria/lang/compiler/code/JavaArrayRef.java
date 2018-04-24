package ria.lang.compiler.code;

import ria.lang.compiler.CType;
import ria.lang.compiler.CodeGen;
import ria.lang.compiler.Context;
import ria.lang.compiler.JavaType;

public final class JavaArrayRef extends Code implements CodeGen {
    Code value, index;
    CType elementType;
    int line;

    public JavaArrayRef(CType _type, Code _value, Code _index, int _line) {
        type = JavaType.convertValueType(elementType = _type);
        value = _value;
        index = _index;
        line = _line;
    }

    private void _gen(Context context, Code store) {
        value.gen(context);
        context.typeInsn(CHECKCAST, JavaType.descriptionOf(value.type));
        index.genInt(context, line, false);
        String resDescr = elementType.javaType == null
            ? JavaType.descriptionOf(elementType)
            : elementType.javaType.description;
        int insn = BALOAD;
        switch(resDescr.charAt(0)) {
            case 'C':
                insn = CALOAD;
                break;
            case 'D':
                insn = DALOAD;
                break;
            case 'F':
                insn = FALOAD;
                break;
            case 'I':
                insn = IALOAD;
                break;
            case 'J':
                insn = LALOAD;
                break;
            case 'S':
                insn = SALOAD;
                break;
            case 'L':
                resDescr = resDescr.substring(1, resDescr.length() - 1);
            case '[':
                insn = AALOAD;
                break;
        }
        if(store != null) {
            insn += 33;
            JavaExpr.genValue(context, store, elementType, line);
            if(insn == AASTORE) {
                context.typeInsn(CHECKCAST, resDescr);
            }
        }
        context.insn(insn);
        if(insn == AALOAD) {
            context.forceType(resDescr);
        }
    }

    @Override
    public void gen(Context context) {
        _gen(context, null);
        JavaExpr.convertValue(context, elementType);
    }

    @Override
    public void gen2(Context context, Code setValue, int line) {
        _gen(context, setValue);
        context.insn(ACONST_NULL);
    }

    @Override
    public Code assign(final Code setValue) {
        return new SimpleCode(this, setValue, null, 0);
    }
}
