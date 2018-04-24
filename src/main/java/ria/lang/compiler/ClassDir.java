package ria.lang.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ClassDir extends ClassPathItem {
    private String path;

    ClassDir(String path) {
        this.path = path;
    }

    @Override
    InputStream getStream(String name, long[] time) throws IOException {
        File f = new File(path, name);
        InputStream r = new FileInputStream(f);
        if(time != null) {
            time[0] = f.lastModified();
        }
        return r;
    }

    @Override
    boolean exists(String name) {
        return new File(path, name).isFile();
    }
}
