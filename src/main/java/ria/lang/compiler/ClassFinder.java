package ria.lang.compiler;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ClassFinder {
    final Map<String, JavaNode> parsed = new HashMap<>();
    final Map<String, Boolean> existsCache = new HashMap<>();
    final String pathStr;
    private final ClassPathItem[] classPath;
    private final ClassPathItem destDir;
    private Map<String, byte[]> defined = new HashMap<>();

    ClassFinder(String cp) {
        this(cp.split(File.pathSeparator), null);
    }

    ClassFinder(String[] cp, String depDestDir) {
        classPath = new ClassPathItem[cp.length];
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < cp.length; ++i) {
            classPath[i] = cp[i].endsWith(".jar")
                ? new ClassJar(cp[i]) : new ClassDir(cp[i]);
            if(i != 0) {
                buf.append(File.pathSeparator);
            }
            buf.append(cp[i]);
        }
        pathStr = buf.toString();
        destDir = depDestDir == null ? null : new ClassDir(depDestDir);
    }

    public InputStream findClass(String name, long[] time) {
        Object x = defined.get(name);
        if(x != null && time != null) {
            time[0] = 0; // unknown, should probably rebuild
            return new ByteArrayInputStream((byte[])x);
        }
        InputStream in;
        for(ClassPathItem aClassPath : classPath) {
            try {
                if((in = aClassPath.getStream(name, time)) != null) {
                    return in;
                }
            } catch(IOException ignored) {
            }
        }
        ClassLoader clc = Thread.currentThread().getContextClassLoader();
        in = clc != null ? clc.getResourceAsStream(name) : null;
        return in != null ? in :
            getClass().getClassLoader().getResourceAsStream(name);
    }

    public void define(String name, byte[] content) {
        defined.put(name, content);
    }

    boolean exists(String name) {
        if(parsed.containsKey(name)) {
            return true;
        }
        Boolean known = existsCache.get(name);
        if(known != null) {
            return known;
        }
        String fn = name.concat(".class");
        boolean found = false;
        for(ClassPathItem aClassPath : classPath) {
            if(aClassPath.exists(fn)) {
                found = true;
                break;
            }
        }
        ClassLoader clc = null;
        InputStream in;
        if(!found) {
            clc = Thread.currentThread().getContextClassLoader();
            if(clc == null && name.startsWith("java")) {
                clc = ClassLoader.getSystemClassLoader();
            }
        }
        if(clc != null && (in = clc.getResourceAsStream(fn)) != null) {
            found = true;
            try {
                in.close();
            } catch(Exception ignored) {
            }
        }
        existsCache.put(name, found);
        return found;
    }

    JavaTypeReader readClass(String className) {
        JavaTypeReader t = new JavaTypeReader();
        t.className = className;
        JavaNode classNode = parsed.get(className);
        if(classNode != null) {
            JavaSource.loadClass(this, t, classNode);
            return t;
        }
        String classFile = className.concat(".class");
        InputStream in = findClass(classFile, null);
        if(in == null) {
            try {
                if(destDir == null) {
                    return null;
                }
                in = destDir.getStream(classFile, null);
            } catch(IOException ex) {
                return null;
            }
        }
        try {
            new ClassReader(in).accept(t, new Attribute[0],
                ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
        } catch(IOException ex) {
            return null;
        } catch(Exception ex) {
            throw new RuntimeException("Internal error reading class " + className + ": " + ex.getMessage(), ex);
        }
        return t;
    }
}
