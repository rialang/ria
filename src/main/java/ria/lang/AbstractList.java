package ria.lang;

/**
 * Ria core library - List.
 */
public abstract class AbstractList extends AbstractIterator implements Comparable<AbstractIterator>, Collection {
    /**
     * Return rest of the list. Must not modify the current list.
     */
    public abstract AbstractList rest();

    public abstract void forEach(Object f);

    public abstract Object fold(Fun f, Object v);

    public abstract AbstractList reverse();

    public abstract RiaNum index(Object v);

    public abstract AbstractList sort();

    public abstract AbstractList smap(Fun f);

    public abstract AbstractList take(int from, int count);

    public AbstractList map(Fun f) {
        return new MapList(this, f);
    }

    public AbstractList find(Fun pred) {
        AbstractList l = this;
        while(l != null && pred.apply(l.first()) != Boolean.TRUE) {
            l = l.rest();
        }
        return l;
    }

    public AbstractList sort(Fun isLess) {
        return new MutableList(this).asort(isLess);
    }

    @Override
    public AbstractList asList() {
        return this;
    }

    @Override
    public void removeAll(AbstractList keys) {
    }

    public String toString() {
        StringBuilder buf = new StringBuilder("[");
        buf.append(Core.show(first()));
        try {
            for(AbstractIterator i = rest(); i != null; i = i.next()) {
                buf.append(',');
                buf.append(Core.show(i.first()));
            }
        } catch(Throwable ignored) {
        }
        buf.append(']');
        return buf.toString();
    }
}
