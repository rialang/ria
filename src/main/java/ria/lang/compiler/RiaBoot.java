package ria.lang.compiler;

import java.io.File;

public class RiaBoot {
    private java.io.File dir;
    private String[] preload = Compiler.PRELOAD;
    private String target;

    public static void main(String[] argv) {
        RiaBoot yb = new RiaBoot();
        yb.dir = new File(argv[0]);
        yb.setDestDir(argv[1]);
        yb.setPreload(argv[2]);

        yb.execute();
    }

    public void setSrcDir(String dir) {
        this.dir = new java.io.File(dir);
    }

    public void setDestDir(String dir) {
        if(dir.length() != 0) {
            dir += '/';
        }
        target = dir;
    }

    public void setPreload(String preload) {
        this.preload = preload.length() == 0
            ? new String[0] : preload.split(":");
    }

    public void execute() {

        final String[] files = dir.list((dir, name) -> name.endsWith(".ria"));

        String[] classPath = new String[0];

        Compiler compilation = new Compiler();
        compilation.writer = new FileWriter(target);
        compilation.depDestDir = target;
        compilation.preload = preload;
        compilation.classPath = new ClassFinder(classPath, target);
        String[] javaOpt = {"-encoding", "utf-8", "-d", target};

        try {
            if(files != null) {
                for(int i = 0; i < files.length; ++i) {
                    files[i] = new File(dir, files[i]).getPath();
                }

                compilation.setSourcePath(new String[]{dir.getPath()});
                compilation.compileAll(files, 0, javaOpt);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
