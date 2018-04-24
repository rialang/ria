package ria.lang.compiler;

import ria.lang.Fun2;

@SuppressWarnings("unused")
class RiaLoader extends Fun2 {
    MemoryClassLoader mem;

    RiaLoader(ClassLoader cl) {
        mem = new MemoryClassLoader(cl);
    }

    @Override
    public Object apply(Object className, Object codeBytes) {
        String name = (String)className;
        byte[] bytes = (byte[])codeBytes;

        // to a dotted classname used by loadClass
        mem.classes.put(name.substring(0, name.length() - 6).replace('/', '.'),
            bytes);
        return null;
    }
}

