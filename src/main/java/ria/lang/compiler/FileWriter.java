package ria.lang.compiler;

import java.io.*;
import ria.lang.Fun2;

class FileWriter extends Fun2 {
    private String target;

    FileWriter(String target) {
        this.target = target;
    }

    @Override
    public Object apply(Object className, Object codeBytes) {
        String name = target + className;
        try {
            byte[] code = (byte[]) codeBytes;
            int sl = name.lastIndexOf('/');
            
            if (sl > 0) {
                new File(name.substring(0, sl)).mkdirs();
            }
            FileOutputStream out = new FileOutputStream(name);
            out.write(code);
            out.close();
        } catch (IOException ex) {
            throw new CompileException(0, 0, "Error writing " + name + ": " + ex.getMessage());
        }
        return null;
    }
}


