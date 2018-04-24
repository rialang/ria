package ria.lang.compiler.code;

import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;

public final class Range extends Code {
    private final Code from;
    private final Code to;

    public Range(Code from, Code to) {
        type = RiaType.NUM_TYPE;
        this.from = from;
        this.to = to;
    }

    @Override
    public void gen(Context context) {
        from.gen(context);
        to.gen(context);
    }
}
