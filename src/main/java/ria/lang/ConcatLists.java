package ria.lang;

import java.io.IOException;
import java.io.OutputStream;

/** Ria core library - Concat list. */
@SuppressWarnings("unused")
final class ConcatLists extends RiaList {
    private AbstractList rest;
    private AbstractIterator src;  // current list?<`a>
    private AbstractIterator tail; // list<list?<`a>>

    public ConcatLists(AbstractIterator src, AbstractIterator rest) {
        super(src.first(), null);
        this.src = src;
        this.tail = rest;
    }

    @Override
    public synchronized AbstractList rest() {
        if (src != null) {
            AbstractIterator i = src.next();
            src = null;
            // current done? -> rest is concatenation of tail list of lists
            //  more current -> rest contains the current
            rest = i == null ? concat(tail) : new ConcatLists(i, tail);
            tail = null;
        }
        return rest;
    }

    @Override
    synchronized AbstractIterator write(OutputStream out) throws IOException {
        if (src == null) {
            return super.write(out);
        }
        AbstractIterator i = src.dup();
        while (i != null) {
            i = i.write(out);
        }
        if (tail != null) {
            AbstractIterator lists = tail.dup();
            do {
                i = (AbstractIterator) lists.first();
                while (i != null) {
                    i = i.write(out);
                }
                lists = lists.next();
            } while (lists != null);
        }
        return null;
    }

    // src is list<list?<'a>>
    public static AbstractList concat(AbstractIterator src) {
        // find first non-empty list in the src list of lists
        while (src != null) {
            AbstractList h = (AbstractList) src.first();
            src = src.next();
            // If found make concat-list mirroring it,
            // with tail src to use when it's finished.
            if (h != null && !h.isEmpty()) {
                return src == null ? h : new ConcatLists(h, src);
            }
        }
        return null; // no, all empty
    }
}
