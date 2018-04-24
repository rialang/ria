package ria.lang;

/** Ria core library - Map list. */
final class MapList extends RiaList {
    private AbstractList rest;
    private AbstractIterator src;
    private final Fun f;

    public MapList(AbstractIterator src, Fun f) {
        super(f.apply(src.first()), null);
        this.src = src;
        this.f = f;
    }

    @Override
    public synchronized AbstractList rest() {
        if (src != null) {
            AbstractIterator i = src.next();
            if (i != null) {
                rest = new MapList(i, f);
            }
            src = null;
        }
        return rest;
    }
}
