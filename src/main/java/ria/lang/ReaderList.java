package ria.lang;

import java.io.BufferedReader;
import java.io.IOException;

/** Ria core library - BufferedReader list. */
final class ReaderList extends RiaList {
    private AbstractList rest;
    private boolean forced;
    private final BufferedReader r;

    private ReaderList(String line, BufferedReader r) {
        super(line, null);
        this.r = r;
    }

    @Override
    public AbstractList rest() {
        synchronized (r) {
            if (!forced) {
                rest = lines(r);
                forced = true;
            }
            return rest;
        }
    }

    static AbstractList lines(BufferedReader r) {
        try {
            String line = null;
            try {
                if ((line = r.readLine()) == null) {
                    return null;
                }
                return new ReaderList(line, r);
            } finally {
                if (line == null) {
                    r.close();
                }
            }
        } catch (IOException ex) {
            //noinspection ThrowableNotThrown
            Unsafe.unsafeThrow(ex);
            // keep compiler happy as we do actually throw at this point
            return null;
        }
    }
}
