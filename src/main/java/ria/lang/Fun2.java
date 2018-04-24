package ria.lang;

public abstract class Fun2 extends Fun {
    @Override
    public abstract Object apply(Object a, Object b);

    @Override
    public Object apply(Object a) {
        return new Fun2_(this, a);
    }
}
