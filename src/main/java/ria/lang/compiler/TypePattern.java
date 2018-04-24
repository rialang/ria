package ria.lang.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TypePattern {
    int var; // if mutable < 0 then match stores type in typeVars as mutable
    TypeWalk end; // end result
    // Integer.MIN_VALUE is type end marker
    // Integer.MAX_VALUE matches any type
    private int[] idx;
    private TypePattern[] next;
    // struct/variant field match, next[idx.length] when no such field
    private String field;
    private boolean mutable;

    TypePattern(int var) {
        this.var = var;
    }

    static TypePattern toPattern(Map<String, CType[]> typedefs) {
        int j = 0, varAlloc = 1;
        TypePattern presult = new TypePattern(varAlloc);
        TypeWalk[] w = new TypeWalk[typedefs.size()];
        Map<CType, TypePattern> tvars = new HashMap<>();
        for(Map.Entry<String, CType[]> o : typedefs.entrySet()) {
            CType[] def = o.getValue();
            CType t = def[def.length - 1].deref();
            if(t.type < RiaType.PRIMITIVES.length) {
                continue;
            }
            for(int k = def.length - 1; --k >= 0; ) {
                tvars.put(def[k].deref(), null); // mark as param
            }
            w[j] = new TypeWalk(t, null, tvars, presult);
            w[j].typename = o.getKey();
            w[j++].def = def;
        }

        if(j == 0) {
            return null;
        }

        TypeWalk[] wg = new TypeWalk[j];
        System.arraycopy(w, 0, wg, 0, j);
        int[] ids = new int[j];
        TypePattern[] patterns = new TypePattern[j];
        List<PatternWalker> walkers = new ArrayList<>();

        walkers.add(new PatternWalker(wg, presult, tvars));

        while(walkers.size() > 0) {
            List<PatternWalker> current = walkers;
            walkers = new ArrayList<>();
            for(PatternWalker aCurrent : current) {
                w = aCurrent.typeWalks;
                Arrays.sort(w);
                // group by different types
                // next - target for group in next cycle
                TypePattern next = new TypePattern(++varAlloc),
                    target = aCurrent.typePattern;
                String field = w.length != 0 ? w[0].field : null;
                int start = 0, n = 0, e;
                for(j = 1; j <= w.length; ++j) {
                    if(j < w.length && w[j].id == w[j - 1].id &&
                        (Objects.equals(field, w[j].field) || field.equals(w[j].field))) {
                        continue; // skip until same
                    }
                    // add branch
                    tvars = new HashMap<>(aCurrent.cTypeTypePatternMap);
                    ids[n] = w[j - 1].id;
                    for(int k = e = start; k < j; ++k) {
                        if((w[e] = w[k].next(tvars, next)) != null) {
                            ++e;
                        }
                    }
                    wg = new TypeWalk[e - start];
                    System.arraycopy(w, start, wg, 0, wg.length);
                    walkers.add(new PatternWalker(wg, patterns[n++] = next, tvars));
                    next = new TypePattern(++varAlloc);
                    start = j;
                    if(j < w.length &&
                        (Objects.equals(field, w[j].field) || field.equals(w[j].field))) {
                        continue; // continue same pattern
                    }
                    target.idx = new int[n];
                    System.arraycopy(ids, 0, target.idx, 0, n);
                    if(field != null) {
                        if(field.charAt(0) == ';') {
                            field = field.substring(1);
                            target.mutable = true;
                        }
                        target.field = field;
                        target.next = new TypePattern[n + 1];
                        System.arraycopy(patterns, 0, target.next, 0, n);
                        if(j < w.length) {
                            field = w[j].field;
                            target.next[n] = next;
                            target = next;
                            next = new TypePattern(++varAlloc);
                        }
                    } else {
                        target.next = new TypePattern[n];
                        System.arraycopy(patterns, 0, target.next, 0, n);
                    }
                    n = 0;
                }
            }
        }
        return presult;
    }

    static TypePattern toPattern(Scope scope, boolean ignoreLocal) {
        Map<String, CType[]> typedefs = new HashMap<>();
        for(; scope != null; scope = scope.outer) {
            CType[] def = scope.typedef(false);
            if(def != null && (!ignoreLocal || scope.name.charAt(0) != '_')) {
                CType[] old = typedefs.put(scope.name, def);
                if(old != null) {
                    typedefs.put(scope.name, old);
                }
            }
        }
        return toPattern(typedefs);
    }

    TypePattern match(CType type, Map<CType, Integer> typeVars) {
        int i;

        type = type.deref();
        Object tv = typeVars.get(type);
        if(tv != null) {
            i = Arrays.binarySearch(idx, (Integer)tv);
            if(i >= 0) {
                return next[i];
            }
        }
        i = Arrays.binarySearch(idx, type.type);
        if(i < 0) {
            if(idx[i = idx.length - 1] != Integer.MAX_VALUE) {
                return null;
            }
            if(var < 0) {
                typeVars.put(type, var);
            }
            return next[i];
        }
        if(var < 0) {
            typeVars.put(type, var);
        }
        TypePattern pat = next[i];
        if(pat.field == null) {
            CType[] param = type.param;
            if(param != null) {
                for(i = 0; i < param.length && pat != null; ++i) {
                    pat = pat.match(param[i], typeVars);
                }
            }
        } else {
            // TODO: check final/partial if necessary
            // Maybe this isn't necessary
            Map<String, CType> m = type.allowedMembers;
            if(m == null) {
                m = type.requiredMembers;
            }
            i = m.size();
            while(--i >= 0 && pat != null) {
                if(pat.field == null) {
                    return null;
                }
                type = m.get(pat.field);
                if(type != null &&
                    type.field == RiaType.FIELD_MUTABLE == pat.mutable) {
                    pat = pat.match(type, typeVars);
                } else {
                    pat = pat.next[pat.idx.length];
                    ++i; // was not matched
                }
            }
        }
        // go for type end marker
        if(pat != null && pat.idx[0] == Integer.MIN_VALUE) {
            return pat.next[0];
        }
        return null;
    }

    private static class PatternWalker {
        TypeWalk[] typeWalks;
        TypePattern typePattern;
        Map<CType, TypePattern> cTypeTypePatternMap;

        PatternWalker(TypeWalk[] typeWalks, TypePattern typePattern, Map<CType, TypePattern> cTypeTypePatternMap) {
            this.typeWalks = typeWalks;
            this.typePattern = typePattern;
            this.cTypeTypePatternMap = cTypeTypePatternMap;
        }
    }
}
