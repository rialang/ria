package ria.lang.compiler.nodes;

public final class TypeDef extends Node {
    public static final int SHARED = 1;
    public static final int OPAQUE = 2;
    public static final int UNSHARE = 3;
    public String name;
    public String[] param;
    public String doc;
    public TypeNode type;
    public int kind;

    @Override
    public String str() {
        StringBuilder buf =
            new StringBuilder("(`typedef ").append(name).append(" (");
        for(int i = 0; i < param.length; ++i) {
            if(i != 0) {
                buf.append(' ');
            }
            buf.append(param[i]);
        }
        return buf.append(") ").append(type.str()).append(')').toString();
    }
}
