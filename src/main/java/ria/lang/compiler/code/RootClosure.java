package ria.lang.compiler.code;

import ria.lang.compiler.Context;
import ria.lang.compiler.ModuleType;

public final class RootClosure extends LoopExpr {
    public LoadModule[] preload;
    public boolean isModule;
    public ModuleType moduleType;
    public int line;

    @Override
    public void gen(Context context) {
        genClosureInit(context);
        for(LoadModule aPreload : preload) {
            if(aPreload != null) {
                aPreload.gen(context);
                context.insn(POP);
            }
        }
        body.gen(context);
    }
}
