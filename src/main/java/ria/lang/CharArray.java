package ria.lang;

public final class CharArray extends RiaArray {
    CharArray(int start, int length, Object array) {
        super(start, length, array);
    }

    @Override
    public Object first() {
        return new String((char[]) array, start, 1);
    }

    @Override
    RiaArray slice(int start, int length) {
        return new CharArray(start, length, array);
    }
}
