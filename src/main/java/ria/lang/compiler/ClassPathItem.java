package ria.lang.compiler;

import java.io.IOException;
import java.io.InputStream;

public abstract class ClassPathItem {
    abstract InputStream getStream(String name, long[] time) throws IOException;

    abstract boolean exists(String name);
}
