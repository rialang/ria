package ria.lang.compiler;

import org.objectweb.asm.ClassWriter;

public final class RiaClassWriter extends ClassWriter {
    RiaClassWriter(int flags) {
        super(COMPUTE_MAXS | flags);
    }

    // Overload to avoid using reflection on non-standard-library classes
    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if(type1.equals(type2)) {
            return type1;
        }
        if(type1.startsWith("java/lang/") && type2.startsWith("java/lang/") ||
            type1.startsWith("ria/lang/") && type2.startsWith("ria/lang/")) {
            return super.getCommonSuperClass(type1, type2);
        }
        return "java/lang/Object";
    }
}
