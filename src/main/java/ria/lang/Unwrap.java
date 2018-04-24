package ria.lang;

import java.util.Objects;

/** Ria core library - Unwrap. */
final class Unwrap extends RiaList {
    private AbstractIterator src;
    private AbstractList rest;

    public Unwrap(Object v, AbstractIterator src) {
        super(v, null);
        this.src = src;
    }

    public static AbstractList filter(AbstractIterator src) {
        Tag t = null;
        while (src != null && !Objects.equals((t = (Tag)src.first()).name, "Some")) {
            src = src.next();
        }
        return src == null ? null : new Unwrap(t.value, src);
    }

    @Override
    public synchronized AbstractList rest() {
        if (src != null) {
            rest = filter(src.next());
            src = null;
        }
        return rest;
    }

    @Override
    public Object copy() {
        return new RiaList(first(), rest());
    }
}
