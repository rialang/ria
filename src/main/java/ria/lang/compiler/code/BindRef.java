package ria.lang.compiler.code;

import ria.lang.compiler.Binder;
import ria.lang.compiler.CType;
import ria.lang.compiler.CaptureWrapper;

public abstract class BindRef extends Code {
    public Binder binder;
    BindExpr.Ref origin;

    // some bindrefs care about being captured. most wont.
    public CaptureWrapper capture() {
        return null;
    }

    // As what java types the values of this should be captured.
    // Same as CaptureWrapper.captureType()
    String captureType() {
        if(origin != null) {
            return ((BindExpr)binder).captureType();
        }
        return 'L' + javaType(type) + ';';
    }

    // unshare. normally bindrefs are not shared
    // Capture shares refs and therefore has to copy for unsharing
    public BindRef unshare() {
        return this;
    }

    Code unref(boolean force) {
        return null;
    }

    // Some bindings can be forced into direct mode
    void forceDirect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Code apply(Code arg, CType res, int line) {
        Apply a = new Apply(res, this, arg, line);
        if((a.ref = origin) != null) {
            origin.arity = 1;
        }
        return a;
    }
}
