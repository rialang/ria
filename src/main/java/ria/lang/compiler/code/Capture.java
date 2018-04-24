package ria.lang.compiler.code;

import ria.lang.compiler.CaptureWrapper;
import ria.lang.compiler.CodeGen;
import ria.lang.compiler.Context;
import ria.lang.compiler.code.BindRef;
import ria.lang.compiler.code.SimpleCode;

public final class Capture extends CaptureRef implements CaptureWrapper, CodeGen {
    String id;
    Capture next;
    CaptureWrapper wrapper;
    Object identity;
    int localVar = -1; // -1 - use this (TryCatch captures use 0 localVar)
    boolean uncaptured;
    boolean ignoreGet;
    private String refType;

    @Override
    public void gen(Context context) {
        if(uncaptured) {
            ref.gen(context);
            return;
        }
        genPreGet(context);
        genGet(context);
    }

    String getId(Context context) {
        if(id == null) {
            id = "_".concat(Integer.toString(context.fieldCounter++));
        }
        return id;
    }

    @Override
    public boolean flagop(int fl) {
        /*
         * DIRECT_BIND is allowed, because with code like
         * x = 1; try x finally end
         * the 1 won't get directly brought into try closure
         * unless the mergeCaptures uncaptures the DIRECT_BIND ones
         * (the variable doesn't (always) know that it will be
         * a direct binding when it's captured, as this determined
         * later using prepareConst())
         * XXX An automatic uncapture is done on DIRECT_BIND, as it allows
         * cascading uncaptures done by mergeCaptures into parent captures,
         * avoiding attempts to generate parent capture wrappings in that case.
         * This fixes 'try-catch class closure' test.
         */
        if(fl == DIRECT_BIND && !uncaptured) {
            return uncaptured = ref.flagop(fl);
        }
        return (fl & (PURE | ASSIGN | DIRECT_BIND)) != 0 && ref.flagop(fl);
    }

    @Override
    public void gen2(Context context, Code value, int __) {
        if(uncaptured) {
            ref.assign(value).gen(context);
        } else {
            genPreGet(context);
            wrapper.genSet(context, value);
            context.insn(ACONST_NULL);
        }
    }

    @Override
    public Code assign(final Code value) {
        if(!ref.flagop(ASSIGN)) {
            return null;
        }
        return new SimpleCode(this, value, null, 0);
    }

    @Override
    public void genPreGet(Context context) {
        if(uncaptured) {
            wrapper.genPreGet(context);
        } else if(localVar < 0) {
            context.load(0);
            if(localVar < -1) {
                context.intConst(-2 - localVar);
                context.insn(AALOAD);
                if(wrapper != null) {
                    String cty = wrapper.captureType();
                    if(cty != null) {
                        context.captureCast(cty);
                    }
                }
            } else {
                context.fieldInsn(GETFIELD, context.className, id, captureType());
            }
        } else {
            context.load(localVar);
            // hacky way to forceType on try-catch, but not on method argument
            if(!ignoreGet) {
                context.forceType(captureType().charAt(0) == '['
                    ? refType : refType.substring(1, refType.length() - 1));
            }
        }
    }

    @Override
    public void genGet(Context context) {
        if(wrapper != null && !ignoreGet) {
            /*
             * The object got from capture might not be the final value.
             * for example captured mutable variables are wrapped into array
             * by the binding, so the wrapper must get correct array index
             * out of the array in that case.
             */
            wrapper.genGet(context);
        }
    }

    @Override
    public void genSet(Context context, Code value) {
        wrapper.genSet(context, value);
    }

    @Override
    public CaptureWrapper capture() {
        if(uncaptured) {
            return ref.capture();
        }
        return wrapper == null ? null : this;
    }

    @Override
    public Object captureIdentity() {
        return wrapper == null ? this : wrapper.captureIdentity();
    }

    @Override
    public String captureType() {
        if(refType == null) {
            if(wrapper != null) {
                refType = wrapper.captureType();
                if(refType == null) {
                    throw new IllegalStateException("captureType:" + wrapper);
                }
            } else if(origin != null) {
                refType = ((BindExpr)binder).captureType();
            } else {
                refType = 'L' + javaType(ref.type) + ';';
            }
        }
        return refType;
    }

    void captureGen(Context context) {
        if(wrapper == null) {
            ref.gen(context);
        } else {
            wrapper.genPreGet(context);
        }
        // stupid AALOAD in genPreGet returns rubbish,
        // so have to captureCast for it...
        context.captureCast(captureType());
    }

    @Override
    public BindRef unshare() {
        return new BindWrapper(this);
    }
}
