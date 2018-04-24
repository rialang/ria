package ria.lang;

@SuppressWarnings("unused")
public final class EscapeFun extends Fun {
    private EscapeError escape;
    private boolean used;
    private Object value;

    private EscapeFun() {
    }

    @Override
    public Object apply(Object arg) {
        if (used) {
            throw new IllegalStateException("exit out of context");
        }
        value = arg;
        throw (escape = new EscapeError());
    }

    public static Object with(Fun f) {
        EscapeFun ef = new EscapeFun();
        try {
            return f.apply(ef);
        } catch (EscapeError e) {
            if (e == ef.escape) {
                return ef.value;
            }
            throw e;
        } finally {
            ef.used = true;
        }
    }
}
