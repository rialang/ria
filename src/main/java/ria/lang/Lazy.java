package ria.lang;

@SuppressWarnings("unused")
class Lazy extends Fun {
    private Object value;
    private Fun f;

    Lazy(Fun f) {
        this.f = f;
    }

    @Override
    public Object apply(Object __) {
        if (f != null) {
            value = f.apply(null);
            f = null;
        }
        return value;
    }
}
