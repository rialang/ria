package ria.lang;

public class AtomicSet extends Fun2 {
    private final Atomic a;
    private final boolean cmp;

    AtomicSet(Atomic a_, boolean cmp_) {
        a = a_;
        cmp = cmp_;
    }

    @Override
    public Object apply(Object arg) {
        if (cmp) {
            return new Fun2_(this, arg);
        }
        return a.getAndSet(arg);
    }

    @Override
    public Object apply(Object expect, Object update) {
        return a.compareAndSet(expect, update) ? Boolean.TRUE : Boolean.FALSE;
    }
}
