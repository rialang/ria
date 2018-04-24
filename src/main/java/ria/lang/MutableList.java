package ria.lang;

import java.util.Arrays;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/** Ria core library - List. */
public class MutableList extends AbstractMutableList implements ByKey {
    private static final Object[] EMPTY = {}; 
    private Object[] array;
    private int size;

    private class SubList extends AbstractMutableList {
        Object first;

        private SubList(int start) {
            this.first = array[start];
            this.start = start;
        }

        @Override
        public Object first() {
            return start < size ? array[start] : first;
        }

        @Override
        public AbstractList rest() {
            int p;
            return (p = start + 1) < size ? new SubList(p) : null;
        }

        @Override
        public AbstractIterator next() {
            int p;
            return (p = start + 1) < size ? new Iter(p) : null;
        }

        @Override
        public boolean isEmpty() {
            return start >= size;
        }

        @Override
        int _size() {
            return size;
        }

        @Override
        Object[] array() {
            return array;
        }

        @Override
        public AbstractList take(int from, int count) {
            if (from < 0) {
                from = 0;
            }
            from += start;
            if (count < 0) {
                return from < size ? from == start
                        ? this : new SubList(from) : null;
            }
            if ((count += start) > size) {
                count = size;
            }
            MutableList res = new MutableList(array);
            res.start = from;
            res.size = count;
            return res;
        }

        @Override
        public AbstractList find(Fun pred) {
            for (int cnt = size, i = start; i < cnt; ++i) {
                if (pred.apply(array[i]) == Boolean.TRUE) {
                    return new SubList(i);
                }
            }
            return null;
        }
    }

    private class Iter extends AbstractIterator implements Serializable {
        private int i;

        private Iter(int start) {
            i = start;
        }

        @Override
        public Object first() {
            if (i >= size) {
                throw new IllegalStateException("End of list reached or list has shrunken.");
            }
            return array[i];
        }

        @Override
        public AbstractIterator next() {
            return ++i < size ? this : null;
        }

        @Override
        public boolean isEmpty() {
            return i >= size;
        }

        @Override
        public AbstractIterator dup() {
            return new Iter(i);
        }

        @Override
        AbstractIterator write(OutputStream out) throws IOException {
            if (i < size) {
                byte[] tmp = new byte[size - i];
                for (int off = i, j = 0; j < tmp.length; ++j) {
                    tmp[j] = ((Number) array[j + off]).byteValue();
                }
                out.write(tmp);
            }
            return null;
        }
    }

    public MutableList() {
        array = EMPTY;
    }

    public MutableList(Object[] array) {
        this.array = array;
        size = array.length;
    }

    public MutableList(AbstractIterator iter) {
        if (iter == null || iter.isEmpty()) {
            array = EMPTY;
        } else {
            array = new Object[10];
            while (iter != null) {
                add(iter.first());
                iter = iter.next();
            }
        }
    }

    // used by setArrayCapacity
    public void reserve(int n) {
        if (n > array.length) {
            Object[] tmp = new Object[n];
            System.arraycopy(array, 0, tmp, 0, size);
            array = tmp;
        }
    }

    // used by push
    public void add(Object o) {
        if (size >= array.length) {
            Object[] tmp = new Object[size == 0 ? 10 : size * 3 / 2 + 1];
            System.arraycopy(array, 0, tmp, 0, array.length);
            array = tmp;
        }
        array[size++] = o;
    }

    // used by shift
    public Object shift() {
        if (start >= size) {
            throw new EmptyArrayException("No first element in empty array");
        }
        return array[start++];
    }

    // used by pop
    public Object pop() {
        if (start >= size) {
            throw new EmptyArrayException("Cannot pop from an empty array");
        }
        return array[--size];
    }

    // used by clear
    public void clear() {
        start = size = 0;
    }

    // used by head
    @Override
    public Object first() {
        if (start >= size) {
            throw new EmptyArrayException("No first element in empty array");
        }
        return array[start];
    }

    // used by tail
    @Override
    public AbstractList rest() {
        int p;
        return (p = start + 1) < size ? new SubList(p) : null;
    }

    // used for iterating lists
    @Override
    public AbstractIterator next() {
        int p;
        return (p = start + 1) < size ? new Iter(p) : null;
    }

    // used by compiler for i in a
    @Override
    public boolean containsKey(Object index) {
        int i;
        return  (i = ((Number) index).intValue()) >= 0 && i + start < size;
    }

    // used by compiler for a[i]
    @Override
    public Object vget(Object index) {
        int i;
        if ((i = ((Number) index).intValue()) < 0) {
            throw new NoSuchKeyException(i, size - start);
        }
        if ((i += start) >= size) {
            throw new NoSuchKeyException(i - start, size - start);
        }
        return array[i];
    }

    // used by compiler for a[number]
    public Object get(int index) {
        int i;
        if (index < 0 || (i = index + start) >= size) {
            throw new NoSuchKeyException(index, size - start);
        }
        return array[i];
    }

    // used by compiler for a[i] := x
    @Override
    public Object put(Object index, Object value) {
        int i;
        if ((i = ((Number) index).intValue()) < 0) {
            throw new NoSuchKeyException(i, size - start);
        }
        if ((i += start) >= size) {
            throw new NoSuchKeyException(i - start, size - start);
        }
        array[i] = value;
        return null;
    }

    // used delete
    @Override
    public Object remove(Object index) {
        int i, n;
        if ((i = ((Number) index).intValue()) < 0) {
            throw new NoSuchKeyException(i, size - start);
        }
        if ((i += start) >= size) {
            throw new NoSuchKeyException(i - start, size - start);
        }
        if ((n = --size - i) > 0) {
            System.arraycopy(array, i + 1, array, i, n);
        }
        return null;
    }

    private void removeRange(ListRange range) {
        int from = range.first.intValue(),
            to = range.last.intValue();
        if (range.inc < 0) {
            int tmp = from;
            from = to;
            to = tmp;
        }
        int n = size - start;
        if (from <= to) {
            if (from < 0 || from >= n) {
                throw new NoSuchKeyException(from, n);
            }
            if (to < 0 || to >= n) {
                throw new NoSuchKeyException(to, n);
            }
            if (++to < n) {
                System.arraycopy(array, to + start,
                                 array, from + start, n - to);
            }
            size -= to - from;
        }
    }

    // used by deleteAll
    @Override
    public void removeAll(AbstractList keys) {
        // a common use case is removing a single range
        if (keys instanceof ListRange) {
            ListRange range = (ListRange) keys;
            if (range.rest == null) {
                removeRange(range);
                return;
            }
        }
        if (keys == null || keys.isEmpty()) {
            return;
        }
        // latter elements must be removed first, so have to sort it
        MutableList kl = new MutableList();
        while (keys != null) {
            kl.add(keys);
            if (keys instanceof ListRange) {
                keys = ((ListRange) keys).rest;
            } else {
                keys = keys.rest();
            }
        }
        Object[] ka = kl.asort().array;
        Object last = null;
        for (int i = kl.size; --i >= 0;) {
            if (ka[i] instanceof ListRange) {
                removeRange((ListRange) ka[i]);
            } else {
                Object index = ((AbstractList) ka[i]).first();
                if (!index.equals(last)) {
                    remove(index);
                    last = index;
                }
            }
        }
    }

    // used by slice
    public MutableList copy(int from, int to) {
        int n = size - start;
        if (from < 0 || from > n) {
            throw new NoSuchKeyException(from, n);
        }
        if (to > n) {
            throw new NoSuchKeyException("Copy range " + from + " to " + to + " exceeds array length " + n);
        }
        if (from >= to) {
            return new MutableList();
        }
        Object[] subArray = new Object[to - from];
        System.arraycopy(array, start + from, subArray, 0, subArray.length);
        return new MutableList(subArray);
    }

    // used by take
    @Override
    public AbstractList take(int from, int count) {
        if (from < 0) {
            from = 0;
        }
        from += start;
        if (count < 0) {
            return from < size ? from == start
                    ? this : new SubList(from) : null;
        }
        if ((count += start) > size) {
            count = size;
        }
        MutableList res = new MutableList(array);
        res.start = from;
        res.size = count;
        return res;
    }

    // used by find
    @Override
    public AbstractList find(Fun pred) {
        for (int cnt = size, i = start; i < cnt; ++i) {
            if (pred.apply(array[i]) == Boolean.TRUE) {
                return new SubList(i);
            }
        }
        return null;
    }

    // used by empty?
    @Override
    public boolean isEmpty() {
        return start >= size;
    }

    @Override
    final int _size() {
        return size;
    }

    @Override
    final Object[] array() {
        return array;
    }

    final MutableList asort() {
        Arrays.sort(array, start, size);
        return this;
    }

    // java sort don't know what the Fun is
    private static void sort(Object[] a, Object[] tmp,
                             int from, int to, Fun isLess) {
        int split = (from + to) / 2;
        if (split - from > 1) {
            sort(tmp, a, from, split, isLess);
        }
        if (to - split > 1) {
            sort(tmp, a, split, to, isLess);
        }
        int i = from, j = split;
        while (i < split && j < to) {
            if (isLess.apply(tmp[i], tmp[j]) == Boolean.TRUE) {
                a[from] = tmp[i++];
            } else {
                a[from] = tmp[j++];
            }
            ++from;
        }
        if (i < split) {
            System.arraycopy(tmp, i, a, from, split - i);
        } else if (j < to) {
            System.arraycopy(tmp, j, a, from, to - j);
        }
    }

    MutableList asort(Fun isLess) {
        if (size - start > 1) {
            Object[] tmp = new Object[size];
            System.arraycopy(array, start, tmp, start, size - start);
            sort(array, tmp, start, size, isLess);
        }
        return this;
    }

    @Override
    public void setDefault(Fun fun) {
        throw new UnsupportedOperationException();
    }

    public Object[] toArray(Object[] to) {
        System.arraycopy(array, start, to, 0, size - start);
        return to;
    }

    public static MutableList ofList(AbstractList list) {
        if (list instanceof MutableList) {
            return (MutableList) list;
        }
        return new MutableList(list);
    }

    public static MutableList ofStrArray(Object[] array) {
        Object[] tmp = new Object[array.length];
        for (int i = 0; i < tmp.length; ++i) {
            tmp[i] = array[i] == null ? Core.UNDEF_STR : array[i];
        }
        return new MutableList(tmp);
    }
}
