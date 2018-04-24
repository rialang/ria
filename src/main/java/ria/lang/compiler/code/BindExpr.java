package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Binder;
import ria.lang.compiler.CaptureWrapper;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

import java.util.Objects;

public final class BindExpr extends SeqExpr implements Binder, CaptureWrapper {
    private final boolean var;
    boolean assigned;
    boolean captured;
    public Ref refs;
    public int evalId = -1;
    private int id;
    private int mvar = -1;
    private String javaType;
    private String javaDescr;
    private Closure closure;
    private boolean directBind;
    private String directField;
    private String myClass;
    private int bindingUsed;

    public BindExpr(Code expr, boolean var) {
        super(expr);
        this.var = var;
    }

    void setMVarId(Closure closure, int arrayId, int index) {
        this.closure = closure;
        mvar = arrayId;
        id = index;
    }

    @Override
    public BindRef getRef(int line) {
        Ref res = new Ref();
        res.binder = this;
        res.type = st.type;
        res.polymorph = !var && st.polymorph;
        res.next = refs;
        if(st instanceof Function) {
            res.origin = res;
        }
        ++bindingUsed;
        return refs = res;
    }

    @Override
    public Object captureIdentity() {
        return mvar == -1 ? this : closure;
    }

    @Override
    public String captureType() {
        if(javaDescr == null) {
            throw new IllegalStateException(toString());
        }
        return mvar == -1 ? javaDescr : "[Ljava/lang/Object;";
    }

    @Override
    public void genPreGet(Context context) {
        if(mvar == -1) {
            if(directField == null) {
                context.load(id);
                int t;
                // garbage collect infinite lists
                if(bindingUsed == 0 && context.tainted == 0 &&
                    ((t = st.type.deref().type) == 0 || t == RiaType.MAP)) {
                    context.insn(ACONST_NULL);
                    context.varInsn(ASTORE, id);
                }
                context.forceType(javaType);
            } else {
                context.fieldInsn(GETSTATIC, myClass, directField, javaDescr);
            }
        } else {
            context.load(mvar).forceType("[Ljava/lang/Object;");
        }
    }

    @Override
    public void genGet(Context context) {
        if(mvar != -1) {
            context.typeInsn(CHECKCAST, "[Ljava/lang/Object;");
            context.intConst(id);
            context.insn(AALOAD);
        }
    }

    @Override
    public void genSet(Context context, Code value) {
        if(directField == null) {
            context.typeInsn(CHECKCAST, "[Ljava/lang/Object;");
            context.intConst(id);
            value.gen(context);
            context.insn(AASTORE);
        } else {
            value.gen(context);
            context.fieldInsn(PUTSTATIC, myClass, directField, javaDescr);
        }
    }

    private void genLocalSet(Context context, Code value) {
        if(mvar == -1) {
            value.gen(context);
            if(!javaType.equals("java/lang/Object")) {
                context.typeInsn(CHECKCAST, javaType);
            }
            if(directField == null) {
                context.varInsn(ASTORE, id);
            } else {
                context.fieldInsn(PUTSTATIC, myClass, directField, javaDescr);
            }
        } else {
            context.load(mvar).intConst(id);
            value.gen(context);
            context.insn(AASTORE);
        }
    }

    // called by Function.prepareConst when this thing mutates into method
    void setCaptureType(String type) {
        javaType = type;
        javaDescr = type.charAt(0) == '[' ? type : 'L' + type + ';';
    }

    public void genBind(Context context) {
        setCaptureType(javaType(st.type));
        if(context == null) {
            return; // named lambdas use genBind for initializing the expr
        }
        if(!var && st.prepareConst(context) && evalId == -1) {
            directBind = true;
            return;
        }
        if(Objects.equals(directField, "")) { // forceDirect, JavaClass does it
            myClass = context.className;
            directField = "$".concat(Integer.toString(context.constants.context.fieldCounter++));
            context.cw.visitField(ACC_STATIC | ACC_SYNTHETIC | ACC_VOLATILE,
                directField, javaDescr, null, null).visitEnd();
        } else if(mvar == -1) {
            id = context.localVarCount++;
        }
        genLocalSet(context, st);
        if(evalId != -1) {
            context.intConst(evalId);
            genPreGet(context);
            if(mvar != -1) {
                context.intConst(id);
            }
            context.methodInsn(INVOKESTATIC,
                "ria/lang/compiler/RiaEval", "setBind",
                mvar == -1 ? "(ILjava/lang/Object;)V"
                    : "(I[Ljava/lang/Object;I)V");
        }
    }

    @Override
    public void gen(Context context) {
        genBind(context);
        result.gen(context);
    }

    @Override
    void genIf(Context context, Label to, boolean ifTrue) {
        genBind(context);
        result.genIf(context, to, ifTrue);
    }

    class Ref extends BindRef {
        int arity;
        Ref next;

        @Override
        public void gen(Context context) {
            if(directBind) {
                st.gen(context);
            } else {
                --bindingUsed;
                genPreGet(context);
                genGet(context);
            }
        }

        @Override
        public Code assign(final Code value) {
            if(!var) {
                return null;
            }
            assigned = true;
            return new Code() {
                @Override
                public void gen(Context context) {
                    genLocalSet(context, value);
                    context.insn(ACONST_NULL);
                }
            };
        }

        @Override
        public boolean flagop(int fl) {
            if((fl & ASSIGN) != 0) {
                if(var) {
                    assigned = true;
                }
                return var;
            }
            if((fl & CONST) != 0) {
                return directBind;
            }
            if((fl & DIRECT_BIND) != 0) {
                return directBind || directField != null;
            }
            if((fl & MODULE_REQUIRED) != 0) {
                return directField != null;
            }
            return (fl & PURE) != 0 && !var;
        }

        @Override
        public CaptureWrapper capture() {
            captured = true;
            if(!var) {
                return null;
            }
            ++bindingUsed; // reference through wrapper
            return BindExpr.this;
        }

        @Override
        Code unref(boolean force) {
            return force || directBind ? st : null;
        }

        @Override
        void forceDirect() {
            directField = "";
        }
    }
}
