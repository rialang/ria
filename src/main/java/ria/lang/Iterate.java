package ria.lang;

/** Ria core library - Iterate function. */
final class Iterate extends RiaList {
    private AbstractList rest;
    private Fun f;

    Iterate(Object v, Fun f) {
        super(v, null);
        this.f = f;
    }

    @Override
    public synchronized AbstractList rest() {
        if (f != null) {
            rest = new Iterate(f.apply(first()), f);
            f = null;
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

    @Override
    public Object copy() {
        return new Iterate(first(), f);
    }
}
