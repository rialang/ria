package ria.lang.compiler;

import org.objectweb.asm.Opcodes;
import ria.lang.compiler.code.BindRef;
import ria.lang.compiler.code.Code;

public final class StructField implements Opcodes {
    public int property; // 0 - not property, 1 - property, -1 - constant property
    public boolean mutable;
    public boolean inherited; // inherited field
    public String name;
    public Code value;
    public Code setter;
    public BindRef binder;
    public String javaName;
    public StructField nextProperty;
    public int index;
    public int line;
}
