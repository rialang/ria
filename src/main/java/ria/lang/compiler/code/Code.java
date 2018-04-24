package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import ria.lang.compiler.CType;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public abstract class Code implements Opcodes {
    // constants used by flagop
    public static final int CONST = 1;
    static final int PURE = 2;

    // for bindrefs, mark as used lvalue
    public static final int ASSIGN = 4;
    static final int INT_NUM = 8;

    // Comparision operators use this for some optimisation.
    static final int EMPTY_LIST = 0x10;

    // Check for no capturing. Here lie dragons
    static final int DIRECT_BIND = 0x20;

    // normal constant is also pure and don't need capturing
    static final int STD_CONST = CONST | PURE | DIRECT_BIND;

    // this which is not captured
    static final int DIRECT_THIS = 0x40;

    // capture that requires bounding function to initialize its module
    static final int MODULE_REQUIRED = 0x80;

    // code object is a list range
    static final int LIST_RANGE = 0x100;

    // Used for mangling names to conform to java naming conventions
    private static final char[] mangle = "jQh$oBz  apCmds          cSlegqt".toCharArray();
    public CType type;
    public boolean polymorph;

    public static String javaType(CType t) {
        t = t.deref();
        switch(t.type) {
            case RiaType.STR:
                return "java/lang/String";
            case RiaType.NUM:
                return "ria/lang/RiaNum";
            case RiaType.CHAR:
                return "java/lang/Character";
            case RiaType.FUN:
                return "ria/lang/Fun";
            case RiaType.STRUCT:
                return "ria/lang/Struct";
            case RiaType.VARIANT:
                return "ria/lang/Tag";
            case RiaType.MAP: {
                int k = t.param[2].deref().type;
                if(k != RiaType.LIST_MARKER) {
                    return "java/lang/Object";
                }
                if(t.param[1].deref().type == RiaType.NUM) {
                    return "ria/lang/MutableList";
                }
                return "ria/lang/AbstractList";
            }
            case RiaType.JAVA:
                return t.javaType.className();
        }
        return "java/lang/Object";
    }

    // Mangle strings with ascii codes between (33 and 64) inclusive
    // as some are not valid java names.
    // This adds a $ sign as an escape character, and certain
    // characters can be represented by themselves.
    public static String mangle(String s) {
        char[] a = s.toCharArray();
        char[] to = new char[a.length * 2];
        int l = 0;
        for(int i = 0, cnt = a.length; i < cnt; ++i, ++l) {
            char c = a[i];
            if(c <= ' ' || c >= 'A' || (to[l + 1] = mangle[c - 33]) == ' ') {
                if(c == '^') {
                    to[l + 1] = 'v';
                } else if(c == '|') {
                    to[l + 1] = 'I';
                } else if(c == '~') {
                    to[l + 1] = '_';
                } else {
                    to[l] = c;
                    continue;
                }
            }
            to[l++] = '$';
        }
        return new String(to, 0, l);
    }

    /**
     * Generates into context a bytecode that (when executed in the JVM)
     * results in a value pushed into stack.
     * That value is the result of evaulating that code snippet.
     */
    public abstract void gen(Context context);

    // Some "functions" may have special kinds of apply
    public Code apply(Code arg, CType res, int line) {
        return new Apply(res, this, arg, line);
    }

    // Binding the second parameter of an operator, for instance
    public Code apply2nd(final Code arg2, final CType t, int line) {
        return new Code() {
            {
                type = t;
            }

            @Override
            public void gen(Context context) {
                context.typeInsn(NEW, "ria/lang/Bind2nd");
                context.insn(DUP);
                Code.this.gen(context);
                arg2.gen(context);
                context.visitInit("ria/lang/Bind2nd", "(Ljava/lang/Object;Ljava/lang/Object;)V");
            }
        };
    }

    // When the code is a lvalue, then this method returns code that
    // performs the lvalue assigment of the value given as argument.
    public Code assign(Code value) {
        return null;
    }

    // Boolean codes have ability to generate jumps.
    void genIf(Context context, Label to, boolean ifTrue) {
        gen(context);
        context.fieldInsn(GETSTATIC, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;");
        context.jumpInsn(ifTrue ? IF_ACMPEQ : IF_ACMPNE, to);
    }

    // should be used, if only int is ever needed
    void genInt(Context context, int line, boolean longValue) {
        gen(context);
        context.visitLine(line);
        context.typeInsn(CHECKCAST, "ria/lang/RiaNum");
        if(longValue) {
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/RiaNum", "longValue", "()J");
        } else {
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/RiaNum", "intValue", "()I");
        }
    }

    // Used to tell that this code is at tail position in a function.
    // Useful for doing tail call optimisations.
    public void markTail() {
    }

    public boolean flagop(int flag) {
        return false;
    }

    // Used for sharing embedded constant objects
    Object valueKey() {
        return this;
    }

    // Called by bind for direct bindings
    // bindings can use this for "preparation"
    boolean prepareConst(Context context) {
        return flagop(CONST);
    }
}
