package ria.lang.compiler.code;

import ria.lang.compiler.Binder;
import ria.lang.compiler.CType;
import ria.lang.compiler.Context;
import ria.lang.compiler.ModuleType;
import ria.lang.compiler.RiaType;

public final class LoadModule extends Code {
    public String moduleName;
    public ModuleType moduleType;
    public boolean checkUsed;
    public boolean typedefUsed;
    private boolean used;

    public LoadModule(String moduleName, ModuleType type, int depth) {
        this.type = type.copy(depth, null);
        this.moduleName = moduleName.toLowerCase();
        moduleType = type;
        polymorph = true;
    }

    @Override
    public void gen(Context context) {
        if(checkUsed && !used) {
            context.insn(ACONST_NULL);
        } else {
            context.methodInsn(INVOKESTATIC, moduleName,
                "eval", "()Ljava/lang/Object;");
        }
    }

    public Binder bindField(final String name, final CType type) {
        return new Binder() {
            @Override
            public BindRef getRef(final int line) {
                final boolean mutable = type.field == RiaType.FIELD_MUTABLE;
                if(!mutable && moduleType.directFields) {
                    String fname = name.equals("eval") ? "eval$" : mangle(name);
                    StaticRef r = new StaticRef(moduleName, fname, type, this, true, line);
                    r.method = true;
                    return r;
                }

                // property or mutable field
                // TODO: reading properties can be done directly
                used = true;
                return new SelectMember(type, LoadModule.this, name, line, !mutable) {

                    @Override
                    public boolean mayAssign() {
                        return mutable;
                    }

                    @Override
                    public boolean flagop(int fl) {
                        return (fl & DIRECT_BIND) != 0 || (fl & ASSIGN) != 0 && mutable;
                    }
                };
            }
        };
    }
}
