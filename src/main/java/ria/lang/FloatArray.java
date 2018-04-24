package ria.lang;

import java.lang.reflect.Array;

public final class FloatArray extends RiaArray {
    FloatArray(int start, int length, Object array) {
        super(start, length, array);
    }

    @Override
    public Object first() {
        return new FloatNum(Array.getDouble(array, start));
    }

    @Override
    RiaArray slice(int start, int length) {
        return new FloatArray(start, length, array);
    }
}
