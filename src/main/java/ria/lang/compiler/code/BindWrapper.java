package ria.lang.compiler.code;

import ria.lang.compiler.CaptureWrapper;
import ria.lang.compiler.Context;

public final class BindWrapper extends BindRef {
    private BindRef ref;

    BindWrapper(BindRef ref) {
        this.ref = ref;
        this.binder = ref.binder;
        this.type = ref.type;
        this.polymorph = ref.polymorph;
        this.origin = ref.origin;
    }

    @Override
    public CaptureWrapper capture() {
        return ref.capture();
    }

    @Override
    public boolean flagop(int fl) {
        return (fl & (PURE | ASSIGN | DIRECT_BIND)) != 0 && ref.flagop(fl);
    }

    @Override
    public void gen(Context context) {
        ref.gen(context);
    }
}
