package ria.lang;

/**
 * Ria core library - No such key exception.
 */
public class NoSuchKeyException extends IndexOutOfBoundsException {
    public NoSuchKeyException(String message) {
        super(message);
    }

    public NoSuchKeyException(int i, int size) {
        super("Array index " + i + (size > 0 ? " out of range 0.." + (size - 1) : " out of empty range"));
    }
}
