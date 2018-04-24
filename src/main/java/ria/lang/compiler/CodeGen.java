package ria.lang.compiler;

import ria.lang.compiler.code.Code;

public interface CodeGen {
    void gen2(Context context, Code param, int line);
}
