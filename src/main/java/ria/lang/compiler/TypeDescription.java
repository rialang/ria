package ria.lang.compiler;

import ria.lang.AbstractList;
import ria.lang.Core;
import ria.lang.GenericStruct;
import ria.lang.RiaList;
import ria.lang.MutableList;
import ria.lang.Struct;
import ria.lang.Struct3;
import ria.lang.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeDescription extends RiaType {
    private int type;
    private String name;
    private TypeDescription value;
    private TypeDescription prev;
    private String alias;
    private Map<String, Object> properties;

    TypeDescription(String name_) {
        name = name_;
    }

    static Struct pair(String name1, Object value1,
                       String name2, Object value2) {
        // low-level implementation-specific struct, don't do that ;)
        Struct3 result = new Struct3(new String[]{name1, name2}, null);
        result._0 = value1;
        result._1 = value2;
        return result;
    }

    static Tag riaType(CType t, TypePattern defs, TypeException path) {
        DescribeContext ctx = new DescribeContext();
        ctx.defs = defs;
        if(path != null) {
            ctx.trace = path.trace;
        }
        return prepare(t, ctx).force();
    }

    static Tag typeDef(CType[] def, MutableList param, TypePattern defs) {
        DescribeContext ctx = new DescribeContext();
        ctx.defs = defs;
        for(int i = 0, n = 0; i < def.length - 1; ++i) {
            // the .doc don't work
            //String name = def[i].doc instanceof String
            //    ? (String) def[i].doc : "t" + ++n;
            String name = "t" + ++n;
            ctx.vars.put(def[i].deref(), name);
            param.add(name);
        }
        return prepare(def[def.length - 1], ctx).force();
    }

    private static void hdescr(TypeDescription descr, CType tt, DescribeContext ctx) {
        Map<String, CType> m = new java.util.TreeMap<>();
        if(tt.requiredMembers != null) {
            m.putAll(tt.requiredMembers);
        }
        if(tt.allowedMembers != null) {
            for(Map.Entry<String, CType> o : tt.allowedMembers.entrySet()) {
                CType t = o.getValue();
                Object v = m.put(o.getKey(), t);
                if(v != null && t.doc == null) {
                    t.doc = v;
                }
            }
        }
        String name;
        // Stupid list is used, because normally it shouldn't ever contain
        // over 1 or 2 elements, and it's faster than hash in this case.
        List<Object> strip = null;
        if(ctx.trace != null) {
            for(int i = 0, last = ctx.trace.size() - 3; i <= last; i += 3) {
                if(ctx.trace.get(i + 1) == tt || ctx.trace.get(i + 2) == tt) {
                    if(strip == null) {
                        strip = new ArrayList<>();
                    }
                    name = (String)ctx.trace.get(i);
                    if(m.containsKey(name) &&
                        !strip.contains(name)) {
                        strip.add(name);
                    }
                }
            }
        }
        if(strip != null && strip.size() >= m.size()) {
            strip = null; // everything is included, no stripping actually
        }
        for(Map.Entry<String, CType> e : m.entrySet()) {
            name = e.getKey();
            if(strip != null && !strip.contains(name)) {
                continue;
            }
            CType t = e.getValue();
            Map<String, Object> it = new HashMap<>(5);
            String doc = t.doc();
            it.put("name", name);
            it.put("description", doc == null ? Core.UNDEF_STR : doc);
            it.put("mutable", t.field == FIELD_MUTABLE);
            it.put("tag",
                tt.allowedMembers == null || !tt.allowedMembers.containsKey(name)
                    ? tt.type == STRUCT ? "." : "" :
                    tt.requiredMembers != null && tt.requiredMembers.containsKey(name)
                        ? "`" : tt.type == STRUCT ? "" : ".");
            it.put("strip", strip);
            TypeDescription field = prepare(t, ctx);
            field.properties = it;
            field.prev = descr.value;
            descr.value = field;
        }
    }

    private static boolean match(TypeDescription descr, CType t, DescribeContext ctx) {
        Map<CType, Integer> defVars;
        TypePattern def;
        if(ctx.defs == null ||
            (def = ctx.defs.match(t, defVars = new HashMap<>())) == null
            || def.end == null) {
            return false;
        }
        descr.name = def.end.typename;
        if(def.end.defvars.length == 0) {
            descr.type = 0;
            return true;
        }
        ctx.refs.put(t, descr); // to avoid infinite recursion
        descr.type = MAP; // Parametric
        Map<Integer, CType> param = new HashMap<>();
        for(Map.Entry<CType, Integer> o : defVars.entrySet()) {
            param.put(o.getValue(), o.getKey());
        }
        for(int i = def.end.defvars.length; --i >= 0; ) {
            CType tp = param.get(def.end.defvars[i]);
            TypeDescription item = tp != null ? prepare(tp, ctx) : new TypeDescription("?");
            item.prev = descr.value;
            descr.value = item;
        }
        if(descr.alias == null) // no recursive refs in parameters?
        {
            ctx.refs.remove(t);
        }
        return true;
    }

    private static TypeDescription prepare(CType t, DescribeContext ctx) {
        final int type = t.type;
        if(type == VAR) {
            if(t.ref != null) {
                return prepare(t.ref, ctx);
            }
            return new TypeDescription(ctx.getVarName(t));
        }
        if(type < PRIMITIVES.length) {
            return new TypeDescription(TYPE_NAMES[type]);
        }
        if(type == JAVA) {
            return new TypeDescription(t.javaType.str());
        }
        if(type == JAVA_ARRAY) {
            return new TypeDescription(prepare(t.param[0], ctx).name.concat("[]"));
        }
        TypeDescription descr = ctx.refs.get(t), item, tmp;
        if(descr != null) {
            if(descr.alias == null) {
                descr.alias = ctx.getVarName(t);
            }
            return new TypeDescription(descr.alias);
        }
        final CType tt = t;
        descr = new TypeDescription(null);
        int varcount = ctx.vars.size();
        if(match(descr, tt, ctx)) {
            return descr;
        }
        ctx.refs.put(tt, descr);
        descr.type = type;
        CType[] param = t.param;
        int n = 1;
        switch(type) {
            case FUN:
                tmp = new TypeDescription(null);
                do {
                    param = t.param;
                    item = prepare(param[0], ctx);
                    item.prev = descr.value;
                    descr.value = item;
                    t = param[1].deref();
                } while(t.type == FUN ? !match(tmp, t, ctx)
                    : (tmp = prepare(t, ctx)) == null);
                tmp.prev = descr.value;
                descr.value = tmp;
                break;
            case STRUCT:
            case VARIANT:
                hdescr(descr, t, ctx);
                t = t.param[0].deref();
                if((t.flags & FL_ERROR_IS_HERE) != 0) {
                    descr.alias = ctx.getVarName(t);
                }
                break;
            case MAP:
                CType p1 = param[1].deref();
                CType p2 = param[2].deref();
                if(p2.type == LIST_MARKER) {
                    descr.name = p1.type == NONE ? "list" : p1.type == NUM
                        ? "array" : "list?";
                    if((p1.flags & FL_ERROR_IS_HERE) != 0) {
                        descr.name = descr.name.concat("*");
                    }
                } else {
                    descr.name = p2.type == MAP_MARKER || p1.type != NUM
                        && p1.type != VAR ? "hash" : "map";
                    if((p2.flags & FL_ERROR_IS_HERE) != 0) {
                        descr.name = descr.name.concat("*");
                    }
                    n = 2;
                }
                while(--n >= 0) {
                    item = prepare(param[n], ctx);
                    item.prev = descr.value;
                    descr.value = item;
                }
                break;
            default:
                if(type < OPAQUE_TYPES) {
                    descr.name = "?" + type + '?';
                    break;
                }
                descr.type = MAP;
                descr.name = t.requiredMembers.keySet().toString();
                for(n = -1; ++n < param.length; ) {
                    item = prepare(param[n], ctx);
                    item.prev = descr.value;
                    descr.value = item;
                }
        }
        // don't create (`foo is ...) when there is no free variables in ...
        if(varcount == ctx.vars.size() && descr.alias == null) {
            ctx.refs.remove(tt);
        }
        return descr;
    }

    Tag force() {
        if(type == 0) {
            return new Tag(name, "Simple");
        }
        AbstractList l = null;
        for(TypeDescription i = value; i != null; i = i.prev) {
            if(i.properties != null) {
                i.properties.put("type", i.force());
                l = new RiaList(new GenericStruct(i.properties), l);
            } else {
                l = new RiaList(i.force(), l);
            }
        }
        Object val = l;
        String tag = null;
        switch(type) {
            case FUN:
                tag = "Function";
                break;
            case MAP:
                val = pair("params", l, "type", name);
                tag = "Parametric";
                break;
            case STRUCT:
                tag = "Struct";
                break;
            case VARIANT:
                tag = "Variant";
                break;
        }
        Tag res = new Tag(val, tag);
        if(alias == null) {
            return res;
        }
        return new Tag(pair("alias", alias, "type", res), "Alias");
    }
}
