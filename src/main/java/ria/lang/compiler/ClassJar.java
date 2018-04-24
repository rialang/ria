package ria.lang.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ClassJar extends ClassPathItem {
    private JarFile jar;
    private Map<String, ZipEntry> entries;

    ClassJar(String path) {
        try {
            jar = new JarFile(path);
            Enumeration<JarEntry> e = jar.entries();
            entries = new HashMap<>();
            while(e.hasMoreElements()) {
                ZipEntry entry = e.nextElement();
                String name = entry.getName();
                if(name.endsWith(".class")) {
                    entries.put(name, entry);
                }
            }
        } catch(IOException ignored) {
        }
    }

    @Override
    InputStream getStream(String name, long[] time) throws IOException {
        ZipEntry entry = entries.get(name);
        if(entry == null) {
            return null;
        }
        InputStream r = jar.getInputStream(entry);
        if(time != null && (time[0] = entry.getTime()) < 0) {
            time[0] = 0; // unknown, should probably rebuild
        }
        return r;
    }

    @Override
    boolean exists(String name) {
        return entries.containsKey(name);
    }
}
