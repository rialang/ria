package ria.lang;

@SuppressWarnings("unused")
public class Const extends Fun {
    private final Object v;

    public Const(Object value) {
        v = value;
    }

    @Override
    public Object apply(Object arg) {
        return v;
    }
}
