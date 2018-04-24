package ria.lang;

/** Ria core library - takeWhile list. */
@SuppressWarnings("unused")
final class TakeWhile extends RiaList {
    private AbstractList rest;
    private AbstractIterator src;
    private Fun pred;

    private TakeWhile(Object first, AbstractIterator src, Fun pred) {
        super(first, null);
        this.src = src;
        this.pred = pred;
    }

    static AbstractList take(AbstractIterator src, Fun f) {
        Object v = src.first();
        return f.apply(v) == Boolean.TRUE ? new TakeWhile(v, src, f) : null;
    }

    @Override
    public synchronized AbstractList rest() {
        if (pred != null) {
            AbstractIterator i = src.next();
            src = null;
            if (i != null) {
                rest = take(i, pred);
            }
            pred = null;
        }
        return rest;
    }
}
