package ria.lang.compiler.code;

import ria.lang.compiler.Context;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractClosure extends Code implements Closure {
    private List<BindExpr> closureVars = new ArrayList<>();

    @Override
    public void addVar(BindExpr binder) {
        closureVars.add(binder);
    }

    public final void genClosureInit(Context context) {
        int id = -1, mvarcount = 0;
        for(int i = closureVars.size(); --i >= 0; ) {
            BindExpr bind = closureVars.get(i);
            if(bind.assigned && bind.captured) {
                if(id == -1) {
                    id = context.localVarCount++;
                }
                bind.setMVarId(this, id, mvarcount++);
            }
        }
        if(mvarcount > 0) {
            context.intConst(mvarcount);
            context.typeInsn(ANEWARRAY, "java/lang/Object");
            context.varInsn(ASTORE, id);
        }
    }
}
