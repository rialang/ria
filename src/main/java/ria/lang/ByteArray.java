package ria.lang;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public final class ByteArray extends RiaArray {
    private final byte[] a;

    ByteArray(int start, int length, byte[] a) {
        super(start, length, a);
        this.a = a;
    }

    @Override
    public Object first() {
        return new IntNum(a[start] & 0xff);
    }

    @Override
    RiaArray slice(int start, int length) {
        return new ByteArray(start, length, a);
    }

    @Override
    public void forEach(Object f_) {
        Fun f = (Fun) f_;
        for (int i = start; i < length; ++i) {
            f.apply(new IntNum(a[i]));
        }
    }

    @Override
    public Object fold(Fun f_, Object v) {
        for (int i = start; i < length; ++i) {
            v = f_.apply(v, new IntNum(a[i]));
        }
        return v;
    }

    @Override
    public AbstractList reverse() {
        byte[] tmp = new byte[length - start];
        for (int i = 0; i < tmp.length; ++i) {
            tmp[tmp.length - i] = a[i + start];
        }
        return new ByteArray(0, tmp.length, tmp);
    }

    @Override
    public RiaNum index(Object v) {
        int b = ((IntNum) v).intValue();
        for (int i = start; i < length; ++i) {
            if (a[i] == b) {
                return new IntNum(i - start);
            }
        }
        return null;
    }

    @Override
    public AbstractList find(Fun pred) {
        for (int i = start, e = length; i < e; ++i) {
            if (pred.apply(new IntNum(a[i])) == Boolean.TRUE) {
                return new ByteArray(i, e, a);
            }
        }
        return null;
    }

    @Override
    public AbstractList sort() {
        byte[] tmp = new byte[length - start];
        System.arraycopy(a, start, tmp, 0, tmp.length);
        Arrays.sort(tmp);
        return new ByteArray(0, tmp.length, tmp);
    }

    @Override
    public long length() {
        return length - start;
    }

    @Override
    public Object copy() {
        byte[] tmp = new byte[length - start];
        System.arraycopy(a, start, tmp, 0, tmp.length);
        return new ByteArray(0, tmp.length, tmp);
    }

    @Override
    AbstractIterator write(OutputStream out) throws IOException {
        out.write(a, start, length - start);
        return null;
    }

    @Override
    public AbstractList map(Fun f) {
        return smap(f);
    }

    @Override
    public AbstractList sort(Fun isLess) {
        return new MutableList(this).asort(isLess);
    }
}
