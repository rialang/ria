package ria.lang;

/** Ria core library - Map 2 lists. */
@SuppressWarnings("unused")
final class Map2List extends RiaList {
    private AbstractList rest;
    private AbstractIterator src;
    private AbstractIterator src2;
    private Fun f;

    public Map2List(Fun f, AbstractIterator src, AbstractIterator src2) {
        super(f.apply(src.first(), src2.first()), null);
        this.src = src;
        this.src2 = src2;
        this.f = f;
    }

    @Override
    public synchronized AbstractList rest() {
        if (f != null) {
            AbstractIterator i = src.next();
            AbstractIterator j = src2.next();
            if (i != null && j != null) {
                rest = new Map2List(f, i, j);
            }
            src = null;
            src2 = null;
            f = null;
        }
        return rest;
    }
}
