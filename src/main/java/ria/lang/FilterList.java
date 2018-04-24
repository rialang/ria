package ria.lang;

/** Ria core library - Map list. */
@SuppressWarnings("unused")
final class FilterList extends RiaList {
    private AbstractList rest;
    private AbstractIterator src;
    private final Fun f;

    private FilterList(Object v, AbstractIterator src, Fun f) {
        super(v, null);
        this.src = src;
        this.f = f;
    }

    static AbstractList filter(AbstractIterator src, Fun f) {
        Object first = null;
        while (src != null && f.apply(first = src.first()) != Boolean.TRUE) {
            src = src.next();
        }
        return src == null ? null : new FilterList(first, src, f);
    }

    @Override
    public synchronized AbstractList rest() {
        if (src != null) {
            rest = filter(src.next(), f);
            src = null;
        }
        return rest;
    }
}
