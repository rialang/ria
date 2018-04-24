package ria.lang.compiler.nodes;

public final class ObjectRefOp extends BinOp {
    public String name;
    public Node[] arguments;

    public ObjectRefOp(String name, Node[] arguments) {
        super("::", 0, true);
        postfix = true;
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public String str() {
        StringBuilder buf =
            new StringBuilder(right == null ? "<>" : right.str());
        buf.append("::").append(name);
        if(arguments != null) {
            buf.append('(');
            for(int i = 0; i < arguments.length; ++i) {
                if(i != 0) {
                    buf.append(", ");
                }
                buf.append(arguments[i].str());
            }
            buf.append(')');
        }
        return buf.toString();
    }
}
