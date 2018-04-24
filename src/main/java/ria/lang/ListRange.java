package ria.lang;

import java.io.Serializable;

/**
 * Ria core library - List.
 */
public final class ListRange extends AbstractList implements Serializable {
    final RiaNum first;
    final RiaNum last;
    final AbstractList rest;
    final int inc;

    private ListRange(RiaNum first, RiaNum last, AbstractList rest) {
        this.first = first;
        this.last = last;
        this.rest = rest;
        this.inc = 1;
    }

    private ListRange(RiaNum first, RiaNum last, AbstractList rest, int inc) {
        this.first = first;
        this.last = last;
        this.rest = rest;
        this.inc = inc;
    }

    public static AbstractList range(Object first, Object last, AbstractList rest) {
        RiaNum f;
        return (f = (RiaNum)first).compareTo(last) > 0 ? rest
            : new ListRange(f, (RiaNum)last, rest);
    }

    @Override
    public Object first() {
        return first;
    }

    @Override
    public AbstractList rest() {
        RiaNum n;
        if((n = first.add(inc)).compareTo(last) * inc > 0) {
            return rest;
        }
        return new ListRange(n, last, rest, inc);
    }

    @Override
    public AbstractIterator next() {
        RiaNum n = first.add(inc);
        if(n.compareTo(last) * inc > 0) {
            return rest;
        }
        return new RangeIter(n, last, rest, inc);
    }

    @Override
    public AbstractList take(int from, int count) {
        RiaNum n = first;
        if(count == 0) {
            return null;
        }
        if(from > 0) {
            n = n.add(inc * from);
            if(n.compareTo(last) * inc > 0) {
                if(rest == null) {
                    return null;
                }
                return rest.take(from - (last.sub(first).intValue() * inc + 1), count);
            }
        }
        AbstractList tail = null;
        RiaNum last_;
        if(count < 0) {
            last_ = last;
            tail = rest;
        } else {
            last_ = n.add(inc * (count - 1));
            if(last_.compareTo(last) * inc > 0) { // last_ > last -> tail remains
                if(rest != null) {
                    tail = rest.take(0, last_.sub(last).intValue() * inc);
                }
                last_ = last;
            }
        }
        return new ListRange(n, last_, tail, inc);
    }

    public int hashCode() {
        int hashCode = 1;
        for(RiaNum i = first; i.compareTo(last) <= 0; i = i.add(inc)) {
            hashCode = 31 * hashCode + i.hashCode();
        }
        for(AbstractIterator i = rest; i != null; i = i.next()) {
            Object x = i.first();
            hashCode = 31 * hashCode + (x == null ? 0 : x.hashCode());
        }
        return hashCode;
    }

    public boolean equals(Object obj) {
        if(!(obj instanceof AbstractList)) {
            return false;
        }
        AbstractIterator i = (AbstractList)obj, j = this;
        Object x, y;
        while(i != null && j != null &&
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
        while(i != null && j != null) {
            int r;
            if((r = ((Comparable<Object>)i.first()).compareTo(j.first())) != 0) {
                return r;
            }
            i = i.next();
            j = j.next();
        }
        if(i != null) {
            return 1;
        }
        if(j != null) {
            return -1;
        }
        return 0;
    }

    @Override
    public void forEach(Object fun) {
        Fun f = (Fun)fun;
        if(inc > 0 && first.rCompare(Integer.MIN_VALUE) < 0 &&
            last.rCompare(Integer.MAX_VALUE) > 0) {
            if(first.compareTo(last) <= 0) {
                for(int i = first.intValue(), e = last.intValue();
                    i <= e; ++i) {
                    f.apply(new IntNum(i));
                }
            }
        } else if(inc < 0 && first.rCompare(Integer.MAX_VALUE) > 0 &&
            last.rCompare(Integer.MIN_VALUE) < 0) {
            if(first.compareTo(last) >= 0) {
                for(int i = first.intValue(), e = last.intValue();
                    i >= e; --i) {
                    f.apply(new IntNum(i));
                }
            }
        } else {
            for(RiaNum i = first; i.compareTo(last) * inc <= 0; i = i.add(inc)) {
                f.apply(i);
            }
        }
        if(rest != null) {
            rest.forEach(fun);
        }
    }

    @Override
    public Object fold(Fun f, Object v) {
        if(inc > 0 && first.rCompare(Integer.MIN_VALUE) < 0 &&
            last.rCompare(Integer.MAX_VALUE) > 0) {
            if(first.compareTo(last) <= 0) {
                for(int i = first.intValue(), e = last.intValue();
                    i <= e; ++i) {
                    v = f.apply(v, new IntNum(i));
                }
            }
        } else if(inc < 0 && first.rCompare(Integer.MAX_VALUE) > 0 &&
            last.rCompare(Integer.MIN_VALUE) < 0) {
            if(first.compareTo(last) >= 0) {
                for(int i = first.intValue(), e = last.intValue();
                    i >= e; --i) {
                    v = f.apply(v, new IntNum(i));
                }
            }
        } else {
            for(RiaNum i = first; i.compareTo(last) * inc <= 0; i = i.add(inc)) {
                v = f.apply(v, i);
            }
        }
        if(rest == null) {
            return v;
        }
        return rest.fold(f, v);
    }

    @Override
    public AbstractList reverse() {
        AbstractList l = new ListRange(last, first, null, -inc);
        for(AbstractIterator i = rest; i != null; i = i.next()) {
            l = new RiaList(i.first(), l);
        }
        return l;
    }

    @Override
    public AbstractList find(Fun pred) {
        RiaNum j;
        if(inc > 0 && first.rCompare(Integer.MIN_VALUE) < 0 &&
            last.rCompare(Integer.MAX_VALUE) > 0) {
            if(first.compareTo(last) <= 0) {
                for(int i = first.intValue(), e = last.intValue();
                    i <= e; ++i) {
                    j = new IntNum(i);
                    if(pred.apply(j) == Boolean.TRUE) {
                        return new ListRange(j, last, rest);
                    }
                }
            }
        } else {
            for(j = first; j.compareTo(last) * inc <= 0; j = j.add(inc)) {
                if(pred.apply(j) == Boolean.TRUE) {
                    return new ListRange(j, last, rest, inc);
                }
            }
        }
        if(rest == null) {
            return null;
        }
        return rest.find(pred);
    }

    @Override
    public AbstractList smap(Fun f) {
        MutableList l;
        if(inc > 0 && first.rCompare(Integer.MIN_VALUE) < 0 &&
            last.rCompare(Integer.MAX_VALUE) > 0) {
            int i = first.intValue(), e = last.intValue();
            if(i > e) {
                return rest.smap(f);
            }
            l = new MutableList();
            l.reserve(e - i + 1);
            while(i <= e) {
                l.add(f.apply(new IntNum(i++)));
            }
        } else if(inc < 0 && first.rCompare(Integer.MAX_VALUE) > 0 &&
            last.rCompare(Integer.MIN_VALUE) < 0) {
            int i = first.intValue(), e = last.intValue();
            if(i < e) {
                return rest.smap(f);
            }
            l = new MutableList();
            l.reserve(i - e + 1);
            while(i >= e) {
                l.add(f.apply(new IntNum(i--)));
            }
        } else {
            return new MapList(this, f);
        }
        for(AbstractIterator i = rest; i != null; i = i.next()) {
            l.add(f.apply(i.first()));
        }
        return l;
    }

    @Override
    public long length() {
        long n = last.sub(first).longValue() / inc + 1;
        if(n < 0) {
            n = 0;
        }
        if(rest == null) {
            return n;
        }
        return n + rest.length();
    }

    @Override
    public RiaNum index(Object v) {
        if(inc > 0) {
            if(first.compareTo(v) <= 0 && last.compareTo(v) >= 0) {
                return ((RiaNum)v).sub(first);
            }
        } else {
            if(last.compareTo(v) <= 0 && first.compareTo(v) >= 0) {
                return ((RiaNum)v).sub(last);
            }
        }
        if(rest == null) {
            return null;
        }
        RiaNum res;
        if((res = rest.index(v)) == null) {
            return null;
        }
        long n;
        if((n = last.sub(first).longValue() / inc) <= 0) {
            return res;
        }
        return res.add(n + 1);
    }

    @Override
    public AbstractList sort() {
        return rest == null ? inc > 0 ? this : reverse()
            : new MutableList(this).asort();
    }

    @Override
    public Object copy() {
        return new ListRange(first, last, rest, inc);
    }
}
