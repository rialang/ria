package ria.lang.compiler.code;

import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

// Applies the foreach function
public class Foreach extends For {
    public Foreach(int line) {
        super("foreach", RiaType.FOREACH_TYPE, line);
    }

    void genApply2(Context context, Code fun, Code list, int line) {
        super.genApply2(context, list, fun, line);
    }
}
