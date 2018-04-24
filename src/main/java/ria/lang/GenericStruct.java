package ria.lang;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

// GenericStruct can be useful in Java code and for very large Ria
// structures, where pointer scan by field names becomes slow.
public class GenericStruct extends AbstractStruct {
    private final Map<String, Object> impl;
    private final boolean allMutable;

    private static String[] getNames(Map<String, Object> values) {
        String[] result =
            values.keySet().toArray(new String[0]);
        Arrays.sort(result);
        return result;
    }

    /**
     * Construct a structure from Java standard Map.
     * Defaults to all fields being mutable.
     */
    public GenericStruct(Map<String, Object> values) {
        super(getNames(values), null);
        this.impl = values;
        this.allMutable = true; // we don't know, use safe default.
    }

    /**
     * Construct a structure from Java standard Map.
     */
    public GenericStruct(Map<String, Object> values, boolean[] vars) {
        super(getNames(values), vars);
        this.impl = values;
        allMutable = false;
    }

    /**
     * Construct a structure with given fields.
     * Values must be initialized using set.
     */
    public GenericStruct(String[] names, boolean[] vars) {
        super(names, vars);
        impl = new HashMap<>(names.length);
        allMutable = false;
    }

    @Override
    public Object get(String field) {
        return impl.get(field);
    }

    @Override
    public Object get(int field) {
        return impl.get(name(field));
    }

    @Override
    public void set(String field, Object value) {
        impl.put(field, value);
    }
    
    @Override
    public Object ref(int field, int[] index, int at) {
        if (!allMutable) {
            return super.ref(field, index, at);
        }
        index[at] = field;
        index[at + 1] = 0;
        return this;
    }
}
