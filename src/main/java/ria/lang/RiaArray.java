package ria.lang;

import java.lang.reflect.Array;

@SuppressWarnings("unused")
public class RiaArray extends RiaList {
    int start;
    final int length;
    final Object array;
    private boolean iter;

    RiaArray(int start, int length, Object array) {
        super(null, null);
        this.start = start;
        this.length = length;
        this.array = array;
    }

    @Override
    public Object first() {
        return new IntNum(Array.getLong(array, start));
    }

    @Override
    public AbstractIterator next() {
        if (iter) {
            return ++start >= length ? null : this;
        }
        RiaArray rest = (RiaArray) rest();
        if (rest != null) {
            rest.iter = true;
        }
        return rest;
    }

    @Override
    public AbstractIterator dup() {
         RiaArray slice = slice(start, length);
         slice.iter = true;
         return slice;
    }

    RiaArray slice(int start, int length) {
        return new RiaArray(start, length, array);
    }

    @Override
    public AbstractList rest() {
        int n;
        return (n = start + 1) >= length ? null : slice(n, length);
    }

    @Override
    public AbstractList take(int from, int count) {
        if (from < 0) {
            from = 0;
        }
        from += start;
        if (count < 0 || (count += from) > length) {
            count = length;
        }
        if (from >= count) {
            return null;
        }
        if (from == start && count == length) {
            return this;
        }
        return slice(from, count);
    }

    @Override
    public long length() {
        return length - start;
    }

    public static AbstractList wrap(byte[] array) {
        return array == null || array.length == 0
            ? null : new ByteArray(0, array.length, array);
    }

    public static AbstractList wrap(short[] array) {
        return array == null || array.length == 0
            ? null : new RiaArray(0, array.length, array);
    }

    public static AbstractList wrap(int[] array) {
        return array == null || array.length == 0
            ? null : new RiaArray(0, array.length, array);
    }

    public static AbstractList wrap(long[] array) {
        return array == null || array.length == 0
            ? null : new RiaArray(0, array.length, array);
    }

    public static AbstractList wrap(float[] array) {
        return array == null || array.length == 0
            ? null : new FloatArray(0, array.length, array);
    }

    public static AbstractList wrap(double[] array) {
        return array == null || array.length == 0
            ? null : new FloatArray(0, array.length, array);
    }

    public static AbstractList wrap(boolean[] array) {
        return array == null || array.length == 0
            ? null : new BooleanArray(0, array.length, array);
    }

    public static AbstractList wrap(char[] array) {
        return array == null || array.length == 0
            ? null : new CharArray(0, array.length, array);
    }
}

