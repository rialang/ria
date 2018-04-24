package ria.lang;

@SuppressWarnings("unused")
final class On extends Fun {
    private final Fun f;
    private final Fun g;

    On(Fun _f, Fun _g) {
        f = _f;
        g = _g;
    }

    // currying - calculate first arg
    @Override
    public Object apply(Object arg) {
        return new Compose(f.apply(g.apply(arg)), g);
    }

    // fast path
    @Override
    public Object apply(Object a, Object b) {
        return f.apply(g.apply(a), g.apply(b));
    }
}
