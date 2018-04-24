package ria.lang;

public final class BooleanArray extends RiaArray {
    BooleanArray(int start, int length, Object array) {
        super(start, length, array);
    }

    @Override
    public Object first() {
        return ((boolean[]) array)[start] ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    RiaArray slice(int start, int length) {
        return new BooleanArray(start, length, array);
    }
}
