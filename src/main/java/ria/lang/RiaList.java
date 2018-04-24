package ria.lang;

import java.io.Serializable;

/** Ria core library - Linked List. */
public class RiaList extends AbstractList implements Serializable {
    private final Object first;
    private final AbstractList rest;

    public RiaList(Object first, AbstractList rest) {
        this.first = first;
        this.rest = rest;
    }

    @Override
    public Object first() {
        return first;
    }

    @Override
    public AbstractList rest() {
        return rest;
    }

    /**
     * Iterators next. Default implementation for lists returns rest.
     * Some lists may have more efficient iterator implementation.
     */
    @Override
    public AbstractIterator next() {
        return rest();
    }

    public int hashCode() {
        int hashCode = 1;
        AbstractIterator i = this;
        do {
            Object x = i.first();
            hashCode = 31 * hashCode + (x == null ? 0 : x.hashCode());
        } while ((i = i.next()) != null);      
        return hashCode;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractList)) {
            return false;
        }
        AbstractIterator i = (AbstractList) obj, j = this;
        Object x, y;
        while (i != null && j != null &&
               ((x = i.first()) == (y = j.first()) ||
                x != null && x.equals(y))) {
            i = i.next();
            j = j.next();
        }
        return i == null && j == null;
    }

    @Override
    public int compareTo(AbstractIterator obj) {
        AbstractIterator i = this, j = obj;
        Object o;
        while (i != null && j != null) {
            int r;
            if ((o = i.first()) != null) {
                if ((r = ((Comparable<Object>) o).compareTo(j.first())) != 0) {
                    return r;
                }
            } else if (j.first() != null) {
                return -1;
            }
            i = i.next();
            j = j.next();
        }
        return i != null ? 1 : j != null ? -1 : 0;
    }

    @Override
    public AbstractList reverse() {
        AbstractIterator i;
        if ((i = next()) == null) {
            return this;
        }
        AbstractList l = new RiaList(first(), null);
        do {
            l = new RiaList(i.first(), l);
        } while ((i = i.next()) != null);
        return l;
    }

    @Override
    public RiaNum index(Object v) {
        int n = 0;
        if (v == null) {
            for (AbstractIterator i = this; i != null; i = i.next()) {
                if (i.first() == null) {
                    return new IntNum(n);
                }
                ++n;
            }
            return null;
        }
        for (AbstractIterator i = this; i != null; i = i.next()) {
            if (v.equals(i.first())) {
                return new IntNum(n);
            }
            ++n;
        }
        return null;
    }

    @Override
    public AbstractList sort() {
        return new MutableList(this).asort();
    }

    @Override
    public long length() {
        int c = 0;
        for (AbstractIterator i = this; i != null; i = i.next()) {
            c++;
        }

        return c;
    }

    @Override
    public void forEach(Object fun) {
        Fun f = (Fun) fun;
        for(AbstractIterator i = this; i != null; i = i.next()) {
            f.apply(i.first());
        }
    }

    @Override
    public Object fold(Fun f, Object v) {
        for(AbstractIterator i = this; i != null; i = i.next()) {
            v = f.apply(v, i.first());
        }
        return v;
    }

    @Override
    public AbstractList smap(Fun f) {
        return map(f);
    }
    
    @Override
    public Object copy() {
        return new RiaList(first, rest() != null ? (AbstractList)rest.copy() : null);
    }

    @Override
    public AbstractList take(int from, int count) {
        return new MutableList(this).take(from, count);
    }
}
