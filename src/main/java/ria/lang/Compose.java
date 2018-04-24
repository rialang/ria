package ria.lang;

public final class Compose extends Fun {
    private final Fun f;
    private final Fun g;

    public Compose(Object var1, Object var2) {
        this.f = (Fun)var1;
        this.g = (Fun)var2;
    }

    @Override
    public Object apply(Object o) {
        return this.f.apply(this.g.apply(o));
    }
}
