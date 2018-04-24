package ria.lang;

import java.io.Serializable;
import java.util.Objects;

public abstract class AbstractStruct implements Struct, Serializable {
    final String[] names;
    private final boolean[] vars;

    public AbstractStruct(String[] names, boolean[] vars) {
        this.names = names;
        this.vars = vars;
    }

    @Override
    public int count() {
        return names.length;
    }

    @Override
    public String name(int field) {
        return names[field];
    }

    @Override
    public String eqName(int field) {
        return names[field];
    }

    @Override
    public Object ref(int field, int[] index, int at) {
        index[at + 1] = 0;
        if (vars != null && vars[field]) {
            index[at] = field;
            return this;
        }
        index[at] = -1;
        return get(field);
    }

    @Override
    public void set(String name, Object value) {
        //noinspection ThrowableNotThrown
        Unsafe.unsafeThrow(new NoSuchFieldException(name));
    }

    public int hashCode() {
        int h = 0;
        for (int i = 0, cnt = count(); i < cnt; ++i) {
            String name = eqName(i);
            if (!Objects.equals(name, "")) {
                Object v = get(i);
                h += name.hashCode() ^ (v == null ? 0 : v.hashCode());
            }
        }
        return h;
    }

    public boolean equals(Object o) {
        Struct st = (Struct) o;
        int acnt = count(), bcnt = st.count(), i = 0, j = 0;
        while (i < acnt && j < bcnt) {
            String an, bn;
            if (Objects.equals(an = eqName(i), bn = st.eqName(j)) && !Objects.equals(an, "")) {
                Object a = get(i);
                Object b = st.get(j);
                if (a != b && (a == null || !a.equals(b))) {
                    return false;
                }
            } else {
                int cmp = an.compareTo(bn);
                if (cmp > 0) {
                    --i;
                }
                if (cmp < 0) {
                    --j;
                }
            }
            ++i;
            ++j;
        }
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder().append('{');
        for (int cnt = count(), i = 0; i < cnt; ++i) {
            String name = eqName(i);
            if (!Objects.equals(name, "")) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(name).append('=').append(Core.show(get(i)));
            }
        }
        sb.append('}');
        return sb.toString();
    }
}
