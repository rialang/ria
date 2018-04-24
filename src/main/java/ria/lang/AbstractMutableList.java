package ria.lang;

import java.io.Serializable;
import java.util.Arrays;

public abstract class AbstractMutableList extends AbstractList implements Serializable {
    int start;

    abstract int _size();
    abstract Object[] array();

    // used by length
    @Override
    public long length() {
        int l = _size() - start;
        return l > 0 ? l : 0;
    }

    // used by copy
    @Override
    public Object copy() {
        Object[] a = new Object[_size()];
        System.arraycopy(array(), start, a, 0, a.length);
        return new MutableList(a);
    }

    public int hashCode() {
        int hashCode = 1;
        Object[] array = array();
        for (int cnt = _size(), i = start; i < cnt; ++i) {
            Object x = array[i];
            hashCode = 31 * hashCode + (x == null ? 0 : x.hashCode());
        }
        return hashCode;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return _size() <= start;
        }
        if (obj instanceof AbstractMutableList) {
            AbstractMutableList o = (AbstractMutableList) obj;
            int cnt = _size();
            if (cnt - start != o._size() - o.start) {
                return false;
            }
            Object[] arr = array(), arr_ = o.array();
            for (int i = start, j = o.start; i < cnt; ++i, ++j) {
                Object a = arr[i], b = arr_[j];
                if (a != b && (a == null || !a.equals(b))) {
                    return false;
                }
            }
            return true;
        }
        if (!(obj instanceof AbstractList)) {
            return false;
        }
        Object[] array = array();
        AbstractIterator j = (AbstractList) obj;
        Object x, y;
        for (int cnt = _size(), i = start; i < cnt; ++i) {
            if (j == null ||
                (x = array[i]) != (y = j.first()) &&
                (x == null || !x.equals(y))) {
                return false;
            }
            j = j.next();
        }
        return j == null;
    }

    public String toString() {
        Object[] array = array();
        StringBuilder buf = new StringBuilder("[");
        for (int cnt = _size(), i = start; i < cnt; ++i) {
            if (i > start) {
                buf.append(',');
            }
            buf.append(Core.show(array[i]));
        }
        buf.append(']');
        return buf.toString();
    }

    @Override
    public void forEach(Object fun) {
        Fun f = (Fun) fun;
        Object[] array = array();
        for (int cnt = _size(), i = start; i < cnt; ++i) {
            f.apply(array[i]);
        }
    }

    @Override
    public Object fold(Fun f, Object v) {
        Object[] array = array();
        for (int cnt = _size(), i = start; i < cnt; ++i) {
            v = f.apply(v, array[i]);
        }
        return v;
    }

    @Override
    public RiaNum index(Object v) {
        Object[] array = array();
        int cnt = _size();
        if (v == null) {
            for (int i = start; i < cnt; ++i) {
                if (array[i] == null) {
                    return new IntNum(i - start);
                }
            }
            return null;
        }
        for (int i = start; i < cnt; ++i) {
            if (v.equals(array[i])) {
                return new IntNum(i - start);
            }
        }
        return null;
    }

    @Override
    public AbstractList map(Fun f) {
        int cnt = _size();
        if (start >= cnt) {
            return null;
        }
        Object[] array = array();
        Object[] result = new Object[cnt - start];
        for (int i = start; i < cnt; ++i) {
            result[i - start] = f.apply(array[i]);
        }
        return new MutableList(result);
    }

    @Override
    public AbstractList smap(Fun f) {
        return map(f);
    }

    @Override
    public int compareTo(AbstractIterator obj) {
        Object[] array = array();
        Object o1;
        int cnt = _size(), r, i = start;
        if (!(obj instanceof AbstractMutableList)) {
            AbstractIterator j = obj;
            for (; i < cnt && j != null; ++i) {
                if ((o1 = array[i]) != null) {
                    if ((r = ((Comparable<Object>) o1).compareTo(j.first())) != 0) {
                        return r;
                    }
                } else if (j.first() != null) {
                    return -1;
                }
                j = j.next();
            }
            return j != null ? -1 : i < cnt ? 1 : 0;
        }
        AbstractMutableList o = (AbstractMutableList) obj;
        Object[] array_ = o.array();
        int cnt_ = o._size();
        for (int j = o.start; i < cnt && j < cnt_; ++i, ++j) {
            if ((o1 = array[i]) != null) {
                if ((r = ((Comparable<Object>) o1).compareTo(array_[j])) != 0) {
                    return r;
                }
            } else if (array_[j] != null) {
                return -1;
            }
        }
        return Integer.compare(cnt, cnt_);
    }

    @Override
    public AbstractList reverse() {
        Object[] array = array();
        int end = _size();
        if (end <= start) {
            return null;
        }
        Object[] r = new Object[end - start];
        --end;
        for (int i = 0; i < r.length; ++i) {
            r[i] = array[end - i];
        }
        return new MutableList(r);
    }

    @Override
    public AbstractList sort() {
        int len;
        if ((len = _size() - start) <= 0) {
            return null;
        }
        Object[] a;
        System.arraycopy(array(), start, a = new Object[len], 0, len);
        Arrays.sort(a);
        return new MutableList(a);
    }

    @Override
    public AbstractList sort(Fun isLess) {
        int len;
        if ((len = _size() - start) <= 0) {
            return null;
        }
        Object[] a;
        System.arraycopy(array(), start, a = new Object[len], 0, len);
        return new MutableList(a).asort(isLess);
    }
}
