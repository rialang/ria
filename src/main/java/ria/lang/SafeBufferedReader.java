package ria.lang;

import java.io.IOException;

// Used in io.ria to avoid having to handle exceptions everywhere
@SuppressWarnings("unused")
final class SafeBufferedReader extends java.io.BufferedReader {
    private boolean closed;

    SafeBufferedReader(java.io.Reader in) {
        super(in, 8192);
    }

    @Override
    public String readLine() throws IOException {
        return closed ? null : super.readLine();
    }

    @Override
    public void close() throws IOException {
        closed = true;
        super.close();
    }
}
