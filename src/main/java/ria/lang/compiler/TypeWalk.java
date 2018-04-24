package ria.lang.compiler;

import java.util.Arrays;
import java.util.Map;

public class TypeWalk implements Comparable {
    int id;
    String field;
    TypePattern pattern;
    String typename;
    CType[] def;
    int[] defvars;
    private CType type;
    private int st;
    private TypeWalk parent;
    private String[] fields;
    private Map<String, CType> fieldMap;

    TypeWalk(CType t, TypeWalk parent, Map<CType, TypePattern> tvars, TypePattern p) {
        pattern = p;
        this.parent = parent;
        type = t = t.deref();
        TypePattern tvar = tvars.get(t);
        if(tvar != null) {
            id = tvar.var;
            if(id > 0) {
                tvar.var = id = -id; // mark used
            }
            return;
        }
        id = t.type;
        if(id == RiaType.VAR) {
            if(tvars.containsKey(t)) {
                id = Integer.MAX_VALUE; // typedef parameter - match anything
                if(p != null && p.var >= 0) {
                    p.var = -p.var; // parameters must be saved
                }
            } else if(parent != null && parent.type.type == RiaType.MAP &&
                parent.st > 1 && (parent.st > 2 ||
                parent.type.param[2] == RiaType.LIST_TYPE)) {
                id = Integer.MAX_VALUE; // map kind - match anything
                return; // and don't associate
            }
            tvars.put(t, p);
        } else if(id >= RiaType.PRIMITIVES.length) {
            tvars.put(t, p);
        }
        if(id == RiaType.STRUCT || id == RiaType.VARIANT) {
            fieldMap = t.allowedMembers != null ? t.allowedMembers
                : t.requiredMembers;
            fields = fieldMap.keySet().toArray(new String[0]);
            Arrays.sort(fields);
        }
    }

    TypeWalk next(Map<CType, TypePattern> tvars, TypePattern pattern) {
        if(id < 0 || id == Integer.MAX_VALUE) {
            if(parent != null) {
                return parent.next(tvars, pattern);
            }
            if(def != null) {
                pattern.end = this;
                defvars = new int[def.length - 1];
                for(int i = 0; i < defvars.length; ++i) {
                    if((pattern = tvars.get(def[i])) != null) {
                        defvars[i] = pattern.var;
                    }
                }
            }
            return null;
        }
        if(fields == null) {
            if(type.param != null && st < type.param.length) {
                return new TypeWalk(type.param[st++], this, tvars, pattern);
            }
        } else if(st < fields.length) {
            CType t = fieldMap.get(fields[st]);
            TypeWalk res = new TypeWalk(t, this, tvars, pattern);
            res.field = fields[st++];
            if(t.field == RiaType.FIELD_MUTABLE) {
                res.field = ";".concat(res.field);
            }
            return res;
        }
        field = null;
        id = Integer.MIN_VALUE;
        return this;
    }

    @Override
    public int compareTo(Object o) {
        TypeWalk tw = (TypeWalk)o;
        if(field == null) {
            return tw.field == null ? id - tw.id : 1;
        }
        if(tw.field == null) {
            return -1;
        }
        int cmp = field.compareTo(tw.field);
        return cmp == 0 ? id - tw.id : cmp;
    }
}
