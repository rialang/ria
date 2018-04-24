package ria.lang.compiler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;

public class MemoryClassLoader extends ClassLoader {
    HashMap<String, byte[]> classes = new HashMap<>();

    MemoryClassLoader(ClassLoader cl) {
        super(cl != null ? cl : Thread.currentThread().getContextClassLoader());
    }

    // override loadClass to ensure loading our own class
    // even when it already exists in current classpath
    @Override
    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class loaded = findLoadedClass(name);
        if(loaded == null) {
            byte[] code = classes.get(name);
            if(code == null) {
                return super.loadClass(name, resolve);
            }
            loaded = defineClass(name, code, 0, code.length);
        }
        if(resolve) {
            resolveClass(loaded);
        }
        return loaded;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        if(path.endsWith(".class")) {
            String name = path.substring(0, path.length() - 6).replace('.', '/');
            byte[] code = classes.get(name);
            if(code != null) {
                return new ByteArrayInputStream(code);
            }
        }
        return super.getResourceAsStream(path);
    }
}
