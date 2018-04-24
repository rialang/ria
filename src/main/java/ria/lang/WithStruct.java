package ria.lang;

import java.util.Objects;

@SuppressWarnings("unused")
public class WithStruct extends AbstractStruct {
    private final Object[] values;
    private final int[] index;
    private final int size;

    public WithStruct(Struct src, Struct override,
                      String[] names, boolean allowNew) {
        super(null, null);
        int ac = src.count(), bc = override.count();
        index = new int[(allowNew ? ac + names.length : ac) << 1];
        values = new Object[index.length];
        int i = 0, j = -1, k = 0, n = 0;
        String an = src.name(0), bn;
        while (!Objects.equals(bn = override.name(++j), names[0])) {
        }
        while (an != null || bn != null) {
            int c = an == null ? 1 : bn == null ? -1 : an.compareTo(bn);
            if (c >= 0) { // src >= override - take override
                values[n] = bn;
                values[n + 1] = override.ref(j, index, n);
                if (++k >= names.length) {
                    bn = null;
                } else {
                    while (!Objects.equals(bn = override.name(++j), names[k])) {
                    }
                }
            } else { // src < override - take super
                values[n] = an;
                values[n + 1] = src.ref(i, index, n);
            }
            if (c <= 0) {
                an = ++i >= ac ? null : src.name(i);
            }
            n += 2;
        }
        size = n >>> 1;
    }

    @Override
    public int count() {
        return size;
    }

    @Override
    public String name(int i) {
        return values[i << 1].toString();
    }

    @Override
    public String eqName(int i) {
        return index[(i <<= 1) + 1] == 0 ? values[i].toString() : "";
    }

    @Override
    public Object get(int i) {
        return values[(i << 1) + 1];
    }

    @Override
    public Object get(String field) {
        int id, i = -2;
        while (values[i += 2] != field) {
        }
        if ((id = index[i]) < 0) {
            return values[i + 1];
        }
        return ((Struct) values[i + 1]).get(id);
    }

    @Override
    public void set(String field, Object value) {
        int i = -2;
        while (values[i += 2] != field) {
        }
        ((Struct) values[i + 1]).set(field, value);
    }

    @Override
    public Object ref(int field, int[] index, int at) {
        index[at] = this.index[field <<= 1];
        index[at + 1] = this.index[++field];
        return values[field];
    }
}
