package ria.lang;

import java.io.OutputStream;

/** Ria core library - List. */
public abstract class AbstractIterator {
    /**
     * Return iterators current first.
     */
    public abstract Object first();

    /**
     * Return next iterator or null.
     * May well modify itself and return this.
     */
    public abstract AbstractIterator next();

    public AbstractIterator dup() {
        return this;
    }

    public boolean isEmpty() {
        return false;
    }

    AbstractIterator write(OutputStream stream) throws java.io.IOException {
        stream.write(((Number) first()).intValue());
        return next();
    }
}
