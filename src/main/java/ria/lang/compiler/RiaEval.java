package ria.lang.compiler;

import java.util.ArrayList;
import java.util.List;

public class RiaEval {
    private static ThreadLocal<RiaEval> instance = new ThreadLocal<>();
    List<Binding> bindings = new ArrayList<>();

    static RiaEval get() {
        return instance.get();
    }

    static RiaEval set(RiaEval eval) {
        RiaEval old = instance.get();
        instance.set(eval);
        return old;
    }

    static int registerBind(String name, CType type,
                            boolean mutable, boolean polymorph) {
        Binding bind = new Binding();
        bind.name = name;
        bind.type = type;
        bind.mutable = mutable;
        bind.polymorph = polymorph && !mutable;
        List<Binding> bindings = get().bindings;
        bind.bindId = bindings.size();
        bindings.add(bind);
        return bind.bindId;
    }

    static void registerImport(String name, CType type) {
        Binding bind = new Binding();
        bind.name = name;
        bind.type = type;
        bind.isImport = true;
        get().bindings.add(bind);
    }

    public static void setBind(int binding, Object[] value, int index) {
        Binding bind = (get().bindings.get(binding));
        bind.value = value;
        bind.index = index;
    }

    public static void setBind(int binding, Object value) {
        setBind(binding, new Object[]{value}, 0);
    }

    public static Object[] getBind(int binding) {
        return (get().bindings.get(binding)).value;
    }

    public static class Binding {
        Object[] value;
        public int index;
        public int bindId;
        String name;
        public CType type;
        public boolean mutable;
        public boolean polymorph;
        boolean isImport;

        Object val() {
            return value[index];
        }
    }
}
