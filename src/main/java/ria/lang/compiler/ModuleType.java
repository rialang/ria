package ria.lang.compiler;

import ria.lang.Tag;
import ria.lang.compiler.nodes.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleType extends Node {
    final CType type;
    final Map<String, CType[]> typeDefs;
    public final boolean directFields;
    Scope typeScope;
    String topDoc;
    String name;
    boolean deprecated;
    boolean fromClass;
    boolean hasSource;
    long lastModified;
    private CType[] free;

    ModuleType(CType type, Map<String, CType[]> typeDefs, boolean directFields, int depth) {
        this.typeDefs = typeDefs;
        this.directFields = directFields;
        this.type = copy(depth, type);
    }

    public CType copy(int depth, CType t) {
        if(t == null) {
            t = type;
        }
        if(depth == -1) {
            return t;
        }
        if(free == null) {
            List<CType> freeVars = new ArrayList<>();
            RiaType.getAllTypeVar(freeVars, null, t, false);
            free = freeVars.toArray(new CType[0]);
        }
        return RiaType.copyType(t, RiaType.createFreeVars(free, depth),
            new HashMap<>());
    }

    Tag riaType() {
        return TypeDescription.riaType(type, typeScope != null
            ? TypePattern.toPattern(typeScope, true)
            : TypePattern.toPattern(typeDefs), null);
    }
}
