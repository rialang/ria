package ria.lang;

import java.io.Serializable;

public final class RangeIter extends AbstractIterator implements Serializable {
    final RiaNum last;
    final AbstractList rest;
    final int inc;
    RiaNum n;

    RangeIter(RiaNum n, RiaNum last, AbstractList rest, int inc) {
        this.n = n;
        this.last = last;
        this.rest = rest;
        this.inc = inc;
    }

    @Override
    public Object first() {
        return n;
    }

    @Override
    public AbstractIterator next() {
        return (n = n.add(inc)).compareTo(last) * inc > 0 ? rest : this;
    }

    @Override
    public AbstractIterator dup() {
        return new RangeIter(n, last, rest, inc);
    }
}
