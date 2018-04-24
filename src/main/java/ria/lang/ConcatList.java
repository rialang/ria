package ria.lang;

import java.io.IOException;
import java.io.OutputStream;

/** Ria core library - Concat list. */
@SuppressWarnings("unused")
final class ConcatList extends RiaList {
    private boolean mappedRest;
    private AbstractIterator src;
    private AbstractList tail;

    public ConcatList(AbstractIterator src, AbstractList tail) {
        super(src.first(), null);
        this.src = src;
        this.tail = tail;
    }

    @Override
    public synchronized AbstractIterator next() {
        return getTail();
    }

    @Override
    public synchronized AbstractList rest() {
        return getTail();
    }

    private AbstractList getTail() {
        if (!mappedRest) {
            AbstractIterator i = src.next();
            if (i != null) {
                tail = new ConcatList(i, tail);
            }
            src = null;
            mappedRest = true;
        }
        return tail;
    }

    @Override
    synchronized AbstractIterator write(OutputStream out) throws IOException {
        if (mappedRest) {
            return super.write(out);
        }
        AbstractIterator i = src.dup();
        while (i != null) {
            i = i.write(out);
        }
        return tail.write(out);
    }
}
