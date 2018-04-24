package ria.lang;

public final class Bind2nd extends Fun {
    private final Fun fun;
    private final Object arg2;

    public Bind2nd(Object fun, Object arg2) {
        this.fun = (Fun) fun;
        this.arg2 = arg2;
    }

    @Override
    public Object apply(Object arg) {
        return ((Fun) fun.apply(arg)).apply(arg2);
    }
}
