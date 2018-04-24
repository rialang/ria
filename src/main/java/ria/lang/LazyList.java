package ria.lang;

/** Ria core library - Lazy list. */
@SuppressWarnings("unused")
public final class LazyList extends RiaList {
    private Fun promise;
    private AbstractList rest;

    public LazyList(Object first, Fun rest) {
        super(first, null);
        promise = rest;
    }

    @Override
    public synchronized AbstractList rest() {
        if (promise != null) {
            rest = (AbstractList) promise.apply(null);
            promise = null;
        }
        return rest;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder("[");
        buf.append(Core.show(first()));
        AbstractIterator i = rest();
        for (int n = 0; i != null && ++n <= 100; i = i.next()) {
            buf.append(',');
            buf.append(Core.show(i.first()));
        }
        if (i != null) {
            buf.append("...");
        }
        buf.append(']');
        return buf.toString();
    }
}
