package ria.lang.compiler;

import org.objectweb.asm.Opcodes;
import ria.lang.compiler.code.Code;
import ria.lang.compiler.code.JavaClass;
import ria.lang.compiler.nodes.Node;
import ria.lang.compiler.nodes.ObjectRefOp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JavaType implements Cloneable {
    static final Map<String, String> JAVA_PRIM = new HashMap<>();
    static final String[] NUMBER_TYPES = {
        "Ljava/lang/Byte;",
        "Ljava/lang/Short;",
        "Ljava/lang/Float;",
        "Ljava/lang/Integer;",
        "Ljava/lang/Long;",
        "Ljava/math/BigInteger;",
        "Ljava/math/BigDecimal;"
    };
    // TODO: Check not used and remove
    private static final JavaType[] EMPTY_JTARR = {};
    private static final HashMap<String, JavaType> CACHE = new HashMap<>();

    static {
        Map<String, String> p = JAVA_PRIM;
        p.put("int", "I");
        p.put("long", "J");
        p.put("boolean", "Z");
        p.put("byte", "B");
        p.put("char", "C");
        p.put("double", "D");
        p.put("float", "F");
        p.put("short", "S");
        p.put("number", "Lria/lang/RiaNum;");
    }

    public final String description;
    public int publicMask = Opcodes.ACC_PUBLIC;
    public int access;
    public JavaClass implementation;
    CType[] TRY_SMART =
        {RiaType.BOOL_TYPE, RiaType.STR_TYPE, RiaType.NUM_TYPE};
    private boolean resolved;
    private Map<String, Field> fields;
    private Map<String, Field> staticFields;
    private Method[] methods;
    private Method[] staticMethods;
    private Method[] constructors;
    private JavaType parent;
    private HashMap<String, JavaType> interfaces;

    private JavaType(String description) {
        this.description = description;
    }

    static void checkPackage(Node where, String packageName,
                             String name, String what, String item) {
        if(!JavaType.packageOfClass(name).equals(packageName)) {
            throw new CompileException(where,
                "Non-public " + what + ' ' + name.replace('/', '.')
                    + (item == null ? "" : "::".concat(item))
                    + " cannot be accessed from different package ("
                    + packageName.replace('/', '.') + ")");
        }
    }

    public static String descriptionOf(CType t) {
        if(t.type == RiaType.VAR) {
            if(t.ref != null) {
                return descriptionOf(t.ref);
            }
            return "Ljava/lang/Object;";
        }
        String r = "";
        while(t.type == RiaType.JAVA_ARRAY) {
            r = r.concat("[");
            t = t.param[0];
        }
        if(t.type != RiaType.JAVA) {
            return "Ljava/lang/Object;";
        }
        return r.concat(t.javaType.description);
    }

    public static CType convertValueType(CType t) {
        if(t.type != RiaType.JAVA) {
            return t;
        }
        String descr = t.javaType.description;
        if(Objects.equals(descr, "Ljava/lang/String;") || Objects.equals(descr, "C")) {
            return RiaType.STR_TYPE;
        }
        if(Objects.equals(descr, "Ljava/lang/Boolean;") || Objects.equals(descr, "Z")) {
            return RiaType.BOOL_TYPE;
        }
        if(Objects.equals(descr, "Lria/lang/RiaNum;") ||
            descr.length() == 1 && "BDFIJS".indexOf(descr.charAt(0)) >= 0) {
            return RiaType.NUM_TYPE;
        }
        if(Objects.equals(descr, "V")) {
            return RiaType.UNIT_TYPE;
        }
        return t;
    }

    private static JavaType getClass(CType t) {
        switch(t.type) {
            case RiaType.JAVA:
                return t.javaType;
            case RiaType.NUM:
                return fromDescription("Lria/lang/RiaNum;");
            case RiaType.STR:
                return fromDescription("Ljava/lang/String;");
            case RiaType.BOOL:
                return fromDescription("Ljava/lang/Boolean;");
            case RiaType.MAP:
                switch(t.param[2].type) {
                    case RiaType.LIST_MARKER:
                        return fromDescription(t.param[1].type == RiaType.NUM
                            ? "Lria/lang/MutableList;" : "Lria/lang/AbstractList;");
                    case RiaType.MAP_MARKER:
                        return fromDescription("Ljava/util/Map;");
                }
                return fromDescription("Lria/lang/ByKey;");
            case RiaType.FUN:
                return fromDescription("Lria/lang/Fun;");
            case RiaType.VARIANT:
                return fromDescription("Lria/lang/Tag;");
            case RiaType.STRUCT:
                return fromDescription("Lria/lang/Struct;");
            case RiaType.UNIT:
            case RiaType.VAR:
                return fromDescription("Ljava/lang/Object;");
        }
        return null;
    }

    static void checkUnsafeCast(Node cast, CType from, CType to) {
        if(from.type != RiaType.JAVA && to.type != RiaType.JAVA &&
            to.type != RiaType.JAVA_ARRAY) {
            throw new CompileException(cast,
                "Illegal cast from " + from + " to " + to +
                    " (neither side is java object)");
        }
        JavaType src = getClass(from);
        if(src == null) {
            throw new CompileException(cast, "Illegal cast from " + from);
        }
        JavaType dst = getClass(to);
        if(from.type == RiaType.VAR && (dst == null || !Objects.equals(dst.description, "Ljava/lang/Object;"))) {
            from.type = RiaType.JAVA;
            from.param = RiaType.NO_PARAM;
            from.javaType = fromDescription("Ljava/lang/Object;");
        }
        if(dst == null) {
            if(Objects.equals(src.description, "Ljava/lang/Object;") &&
                to.type == RiaType.JAVA_ARRAY) {
                return;
            }
            throw new CompileException(cast, "Illegal cast to " + to);
        }
        if(to.type == RiaType.JAVA && (src.access & Opcodes.ACC_INTERFACE) != 0) {
            return;
        }
        try {
            if(dst.isAssignable(src) < 0 && (from.type != RiaType.JAVA || src.isAssignable(dst) < 0)) {
                throw new CompileException(cast, "Illegal cast from " + from + " to " + to);
            }
        } catch(JavaClassNotFoundException ex) {
            throw new CompileException(cast, ex);
        }
    }

    public static JavaType createNewClass(String className, JavaClass impl) {
        JavaType t = new JavaType('L' + className + ';');
        t.implementation = impl;
        return t;
    }

    private static void putMethods(Map<String, Method> mm, Method[] methods) {
        for(int i = methods.length; --i >= 0; ) {
            Method m = methods[i];
            mm.put(m.sig, m);
        }
    }

    private static void putMethods(Map<String, Method> mm, List<Method> methods) {
        for(int i = methods.size(); --i >= 0; ) {
            Method m = methods.get(i);
            mm.put(m.sig, m);
        }
    }

    private static Method[] methodArray(Collection<Method> c) {
        return c.toArray(new Method[0]);
    }

    private static int isAssignable(CType to, CType from, boolean smart)
        throws JavaClassNotFoundException, TypeException {
        to = to.deref();
        from = from.deref();
        if(to.type == RiaType.JAVA) {
            return to.javaType.isAssignableJT(to, from, smart);
        }
        if(to.type == RiaType.JAVA_ARRAY) {
            CType of = to.param[0];
            switch(from.type) {
                case RiaType.STR:
                    return of.type == RiaType.JAVA &&
                        Objects.equals(of.javaType.description, "C") ? 1 : -1;
                case RiaType.MAP: {
                    return from.param[2].type == RiaType.LIST_MARKER &&
                        isAssignable(to.param[0], from.param[0], smart)
                            >= 0 ? 1 : -1;
                }
                case RiaType.JAVA_ARRAY:
                    return isAssignable(to.param[0], from.param[0], smart);
            }
        }
        if(to.type == RiaType.STR &&
            from.type == RiaType.JAVA &&
            Objects.equals(from.javaType.description, "Ljava/lang/String;")) {
            return 0;
        }
        return -1;
    }

    static int isAssignable(Node where, CType to, CType from, boolean smart) {
        from = from.deref();
        if(smart && from.type == RiaType.UNIT) {
            return 0;
        }
        try {
            return isAssignable(to, from, smart);
        } catch(JavaClassNotFoundException ex) {
            throw new CompileException(where, ex);
        } catch(TypeException ex) {
            throw new CompileException(where, ex.getMessage());
        }
    }

    static boolean isSafeCast(Scope scope, Node where, CType to, CType from, boolean explicit) {
        to = to.deref();
        from = from.deref();
        // automatic array wrapping
        CType mapKind;
        if(from.type == RiaType.JAVA_ARRAY && to.type == RiaType.MAP &&
            ((mapKind = to.param[2].deref()).type == RiaType.LIST_MARKER ||
                mapKind.type == RiaType.VAR)) {
            CType fp = from.param[0].deref();
            CType tp = to.param[0].deref();
            try {
                if(fp.javaType != null &&
                    fp.javaType.description.length() == 1) {
                    char fromPrimitive = fp.javaType.description.charAt(0);
                    RiaType.unify(to.param[1], RiaType.NO_TYPE);
                    RiaType.unify(to.param[0],
                        fromPrimitive == 'Z' ? RiaType.BOOL_TYPE :
                            fromPrimitive == 'C' ? RiaType.STR_TYPE :
                                RiaType.NUM_TYPE);
                } else if(tp.type == RiaType.VAR) {
                    if(fp != tp) {
                        RiaType.unifyToVar(tp, fp);
                    }
                } else if(isAssignable(where, tp, fp, false) < 0) {
                    return false;
                }
            } catch(TypeException ex) {
                return false;
            }
            mapKind.type = RiaType.LIST_MARKER;
            mapKind.param = RiaType.NO_PARAM;
            CType index = to.param[1].deref();
            if(index.type == RiaType.VAR) {
                index.type = tp.type == RiaType.STR
                    ? RiaType.NONE : RiaType.NUM;
                index.param = RiaType.NO_PARAM;
            }
            if(index.type == RiaType.NUM && tp.type == RiaType.STR) {
                scope.ctx.compiler.warn(new CompileException(where,
                    "Cast `as array<string>' is dangerous and deprecated." +
                        "\n    Please use either `as list<string>' or" +
                        " `as array<~String>'"));
            }
            return true;
        }
        if(to.type == RiaType.JAVA && to.javaType.description.length() == 1) {
            return false;
        }
        if(explicit) {
            return isAssignable(where, to, from, true) >= 0;
        }
        boolean smart = true;
        boolean mayExact = false;
        while(from.type == RiaType.MAP &&
            from.param[2].type == RiaType.LIST_MARKER &&
            (to.type == RiaType.MAP &&
                from.param[1].type == RiaType.NONE &&
                to.param[2].type == RiaType.LIST_MARKER &&
                to.param[1].type != RiaType.NUM ||
                to.type == RiaType.JAVA_ARRAY)) {
            if(to.type == RiaType.JAVA_ARRAY) {
                mayExact = true;
            }
            from = from.param[0].deref();
            to = to.param[0].deref();
            smart = false;
        }
        if(to.type == RiaType.STR && smart &&
            (from.type == RiaType.JAVA &&
                Objects.equals(from.javaType.description, "Ljava/lang/String;"))) {
            return true;
        }
        if(from.type != RiaType.JAVA) {
            return false;
        }
        try {
            return to.type == RiaType.JAVA &&
                (to.javaType != from.javaType || mayExact) &&
                (smart ? isAssignable(where, to, from, true)
                    : to.javaType.isAssignable(from.javaType)) >= 0;
        } catch(JavaClassNotFoundException ex) {
            throw new CompileException(where, ex);
        }
    }

    private static JavaType javaTypeOf(Node where, CType objType, String err) {
        if(objType.type != RiaType.JAVA) {
            throw new CompileException(where, err + objType + ", java object expected");
        }
        return objType.javaType.resolve(where);
    }

    static Method resolveConstructor(Node call, CType t, Code[] args, boolean noAbstract) {
        JavaType jt = t.javaType.resolve(call);
        if((jt.access & Opcodes.ACC_INTERFACE) != 0) {
            throw new CompileException(call, "Cannot instantiate interface " + jt.dottedName());
        }
        if(noAbstract && (jt.access & Opcodes.ACC_ABSTRACT) != 0) {
            StringBuilder msg = new StringBuilder("Cannot construct abstract class ");
            msg.append(jt.dottedName());
            int n = 0;
            for(int i = 0; i < jt.methods.length; ++i) {
                if((jt.methods[i].access & Opcodes.ACC_ABSTRACT) != 0) {
                    if(++n == 1) {
                        msg.append("\nAbstract methods found in ");
                        msg.append(jt.dottedName());
                        msg.append(':');
                    } else if(n > 2) {
                        msg.append("\n    ...");
                        break;
                    }
                    msg.append("\n    ");
                    msg.append(jt.methods[i]);
                }
            }
            throw new CompileException(call, msg.toString());
        }
        return jt.resolveByArgs(call, jt.constructors, "<init>", args, t);
    }

    static Method resolveMethod(ObjectRefOp ref, CType objType, Code[] args, boolean isStatic) {
        objType = objType.deref();
        JavaType jt = javaTypeOf(ref, objType, "Cannot call method on ");
        return jt.resolveByArgs(ref, isStatic ? jt.staticMethods : jt.methods,
            ref.name, args, objType);
    }

    static Field resolveField(ObjectRefOp ref, CType objType, boolean isStatic) {
        objType = objType.deref();
        JavaType jt = javaTypeOf(ref, objType, "Cannot access field on ");
        Map<String, Field> fm = isStatic ? jt.staticFields : jt.fields;
        Field field = fm.get(ref.name);
        if(field == null) {
            throw new CompileException(ref,
                (isStatic ? "Static field " : "Field ") +
                    ref.name + " not found in " + jt.dottedName());
        }
        if(field.classType != objType) {
            if(!field.className.equals(objType.javaType.className())) {
                field = new Field(field.name, field.access,
                    field.className, field.type);
                fm.put(field.name, field);
            }
            field.classType = objType;
        }
        return field;
    }

    static JavaType fromDescription(String sig) {
        synchronized(CACHE) {
            return CACHE.computeIfAbsent(sig, JavaType::new);
        }
    }

    static CType typeOfClass(String packageName, String className) {
        if(packageName != null && packageName.length() != 0) {
            className = packageName + '/' + className;
        }
        return new CType("L" + className + ';');
    }

    static String packageOfClass(String className) {
        if(className == null || className.length() == 0) {
            return "";
        }
        int p = className.lastIndexOf('/');
        return p < 0 ? "" : className.substring(0, p);
    }

    private static List<JavaType> parentList(JavaType t) {
        List<JavaType> a = new ArrayList<>();
        while(t != null) {
            a.add(t);
            t = t.parent;
        }
        return a;
    }

    static CType mergeTypes(CType a, CType b) {
        a = a.deref();
        b = b.deref();
        if(a.type != RiaType.JAVA || b.type != RiaType.JAVA) {
            // immutable lists can be recursively merged
            if(a.type == RiaType.MAP && b.type == RiaType.MAP &&
                a.param[1].type == RiaType.NONE &&
                a.param[2].type == RiaType.LIST_MARKER &&
                b.param[1].type == RiaType.NONE &&
                b.param[2].type == RiaType.LIST_MARKER) {
                CType t = mergeTypes(a.param[0], b.param[0]);
                if(t != null) {
                    return new CType(RiaType.MAP, new CType[]{
                        t, RiaType.NO_TYPE, RiaType.LIST_TYPE});
                }
            }
            if(a.type == RiaType.UNIT &&
                (b.type == RiaType.JAVA &&
                    b.javaType.description.length() != 1 ||
                    b.type == RiaType.JAVA_ARRAY)) {
                return b;
            }
            if(b.type == RiaType.UNIT &&
                (a.type == RiaType.JAVA &&
                    a.javaType.description.length() != 1 ||
                    a.type == RiaType.JAVA_ARRAY)) {
                return a;
            }
            return null;
        }
        if(a.javaType == b.javaType) {
            return a;
        }
        List<JavaType> aa = parentList(a.javaType);
        List<JavaType> ba = parentList(b.javaType);
        JavaType common = null;
        for(int i = aa.size(), j = ba.size();
            --i >= 0 && --j >= 0 && aa.get(i) == ba.get(j); ) {
            common = aa.get(i);
        }
        if(common == null) {
            return null;
        }
        JavaType aj = a.javaType, bj = b.javaType;
        if(Objects.equals(common.description, "Ljava/lang/Object;")) {
            int mc = -1;
            if(bj.interfaces.containsKey(aj.description)) {
                return a;
            }
            if(aj.interfaces.containsKey(bj.description)) {
                return b;
            }
            Map<String, JavaType> m = bj.interfaces;
            for(String o1 : aj.interfaces.keySet()) {
                JavaType o;
                if((o = m.get(o1)) != null) {
                    int n = o.methods.length;
                    if(n > mc) {
                        common = o;
                    }
                }
            }
        }
        CType t = new CType(RiaType.JAVA, RiaType.NO_PARAM);
        t.javaType = common;
        return t;
    }

    static CType typeOfName(String name, Scope scope) {
        int arrays = 0;
        while(name.endsWith("[]")) {
            ++arrays;
            name = name.substring(0, name.length() - 2);
        }
        String descr = JAVA_PRIM.get(name);
        CType t = descr != null ? new CType(descr) :
            RiaType.resolveFullClass(arrays == 0 ? name : name, scope);
        while(--arrays >= 0) {
            t = new CType(RiaType.JAVA_ARRAY, new CType[]{t});
        }
        return t;
    }

    void checkPackage(Node where, String packageName) {
        if((access & Opcodes.ACC_PUBLIC) == 0) {
            checkPackage(where, packageName, className(),
                (access & Opcodes.ACC_INTERFACE) != 0
                    ? "interface" : "class", null);
        }
    }

    public boolean isInterface() {
        return (access & Opcodes.ACC_INTERFACE) != 0;
    }

    public boolean isCollection() {
        return Objects.equals(description, "Ljava/util/List;") ||
            Objects.equals(description, "Ljava/util/Collection;") ||
            Objects.equals(description, "Ljava/util/Set;");
    }

    public String className() {
        if(!description.startsWith("L")) {
            throw new RuntimeException("No className for " + description);
        }
        return description.substring(1, description.length() - 1);
    }

    public String dottedName() {
        return className().replace('/', '.');
    }

    private synchronized void resolve() throws JavaClassNotFoundException {
        if(resolved) {
            return;
        }
        if(!description.startsWith("L")) {
            resolved = true;
            return;
        }
        JavaTypeReader t = Compiler.currentCompiler.get()
            .classPath.readClass(className());
        if(t == null) {
            throw new JavaClassNotFoundException(dottedName());
        }
        resolve(t);
    }

    public void resolve(JavaTypeReader t) throws JavaClassNotFoundException {
        access = t.access;
        interfaces = new HashMap<>();
        if(t.interfaces != null) {
            for(int i = t.interfaces.length; --i >= 0; ) {
                JavaType it = fromDescription('L' + t.interfaces[i] + ';');
                it.resolve();
                interfaces.putAll(it.interfaces);
                interfaces.put(it.description, it);
            }
        }
        fields = new HashMap<>();
        staticFields = new HashMap<>();
        HashMap<String, Method> mm = new HashMap<>();
        HashMap<String, Method> smm = new HashMap<>();
        if(t.parent != null) {
            parent = t.parent;
            parent.resolve();
        }
        for(Object o : interfaces.values()) {
            JavaType ii = (JavaType)o;
            staticFields.putAll(ii.staticFields);
            putMethods(mm, ii.methods);
        }
        if(parent != null) {
            interfaces.putAll(parent.interfaces);
            fields.putAll(parent.fields);
            staticFields.putAll(parent.staticFields);
            putMethods(mm, parent.methods);
            putMethods(smm, parent.staticMethods);
        }
        fields.putAll(t.fields);
        staticFields.putAll(t.staticFields);
        putMethods(mm, t.methods);
        putMethods(smm, t.staticMethods);
        constructors = methodArray(t.constructors);
        methods = methodArray(mm.values());
        staticMethods = methodArray(smm.values());
        resolved = true;
    }

    void checkAbstract() {
        if((access & Opcodes.ACC_ABSTRACT) != 0) {
            return;
        }
        for(int i = methods.length; --i >= 0; ) {
            if((methods[i].access & Opcodes.ACC_ABSTRACT) != 0) {
                access |= Opcodes.ACC_ABSTRACT;
                return;
            }
        }
    }

    int isAssignable(JavaType from) throws JavaClassNotFoundException {
        from.resolve();
        if(this == from) {
            return 0;
        }
        if(from.description.length() == 1) {
            return -1;
        }
        if(from.interfaces.containsKey(description)) {
            return 1; // I'm an interface implemented by from
        }
        from = from.parent;
        // I'm from or one of from's parents?
        for(int i = 1; from != null; ++i) {
            if(this == from) {
                return i;
            }
            from.resolve();
            from = from.parent;
        }
        return -1;
    }

    // -1 not assignable. 0 - perfect match. > 0 convertable.
    private int isAssignableJT(CType to, CType from, boolean smart)
        throws JavaClassNotFoundException, TypeException {
        int ass;
        if(from.type != RiaType.JAVA && Objects.equals(description, "Ljava/lang/Object;")) {
            return from.type == RiaType.VAR ? 1 : 10;
        }
        switch(from.type) {
            case RiaType.STR:
                switch(description) {
                    case "Ljava/lang/String;":
                        return 0;
                    case "Ljava/lang/CharSequence;":
                        return 1;
                    case "Ljava/lang/StringBuffer;":
                    case "Ljava/lang/StringBuilder;":
                        return 2;
                    default:
                        return Objects.equals("C", description) ? 3 : -1;
                }
            case RiaType.NUM:
                switch(description) {
                    case "D":
                    case "Ljava/lang/Double;":
                        return 3;
                    case "B":
                    case "F":
                    case "I":
                    case "J":
                    case "S":
                        return 4;
                    case "Ljava/lang/Number;":
                        return 1;
                    case "Lria/lang/RiaNum;":
                        return 0;
                    default:
                        for(int i = NUMBER_TYPES.length; --i >= 0; ) {
                            if(Objects.equals(NUMBER_TYPES[i], description)) {
                                return 4;
                            }
                        }
                        return -1;
                }
            case RiaType.BOOL:
                return Objects.equals(description, "Z") || Objects.equals(description, "Ljava/lang/Boolean;")
                    ? 0 : -1;
            case RiaType.FUN:
                return Objects.equals(description, "Lria/lang/Fun;") ? 0 : -1;
            case RiaType.MAP: {
                switch(from.param[2].deref().type) {
                    case RiaType.MAP_MARKER:
                        return Objects.equals("Ljava/util/Map;", description) &&
                            (to.param.length == 0 ||
                                isAssignable(to.param[1], from.param[0], smart) == 0 &&
                                    isAssignable(from.param[1], to.param[0], smart) >= 0)
                            ? 0 : -1;
                    case RiaType.LIST_MARKER:
                        if(Objects.equals("Ljava/util/List;", description) ||
                            Objects.equals("Ljava/util/Collection;", description) ||
                            Objects.equals("Ljava/util/Set;", description) ||
                            Objects.equals("Lria/lang/AbstractList;", description) ||
                            Objects.equals("Lria/lang/AbstractIterator;", description)) {
                            break;
                        }
                    default:
                        return -1;
                }
                return to.param.length == 0 ||
                    (ass = isAssignable(to.param[0], from.param[0], smart)) == 0
                    || ass > 0 && from.param[1].type == RiaType.NONE ? 1 : -1;
            }
            case RiaType.STRUCT:
                return Objects.equals(description, "Lria/lang/Struct;") ? 0 : -1;
            case RiaType.VARIANT:
                return Objects.equals(description, "Lria/lang/Tag;") ? 0 : -1;
            case RiaType.JAVA:
                return isAssignable(from.javaType);
            case RiaType.JAVA_ARRAY:
                return (Objects.equals("Ljava/util/Collection;", description) ||
                    Objects.equals("Ljava/util/List;", description)) &&
                    (to.param.length == 0 ||
                        isAssignable(to.param[0], from.param[0], smart) == 0)
                    ? 1 : -1;
            case RiaType.VAR:
                if(smart) {
                    for(CType aTRY_SMART : TRY_SMART) {
                        int r = isAssignableJT(to, aTRY_SMART, false);
                        if(r >= 0) {
                            RiaType.unify(from, aTRY_SMART);
                            return r;
                        }
                    }
                    RiaType.unify(from, to);
                    return 1;
                }
        }
        return Objects.equals(description, "Ljava/lang/Object;") ? 10 : -1;
    }

    private Method resolveByArgs(Node n, Method[] ma, String name, Code[] args, CType objType) {
        int rAss = Integer.MAX_VALUE;
        int res = -1;
        int suitable[] = new int[ma.length];
        int suitableCounter = 0;
        for(int i = ma.length; --i >= 0; ) {
            Method m = ma[i];
            if(Objects.equals(m.name, name) && m.arguments.length == args.length) {
                suitable[suitableCounter++] = i;
            }
        }
        boolean single = suitableCounter == 1;
        find_match:
        while(--suitableCounter >= 0) {
            int index = suitable[suitableCounter];
            Method m = ma[index];
            int mAss = 0;
            for(int j = 0; j < args.length; ++j) {
                int ass = isAssignable(n, m.arguments[j], args[j].type, single);
                if(ass < 0) {
                    continue find_match;
                }
                if(ass != 0) {
                    mAss += ass + 1;
                }
            }
            if(m.returnType.javaType != null &&
                (m.returnType.javaType.resolve(n).access &
                    Opcodes.ACC_PUBLIC) == 0) {
                mAss += 10;
            }
            if(mAss == 0) {
                res = index;
                break;
            }
            if(mAss < rAss) {
                res = index;
                rAss = mAss;
            }
        }
        if(res != -1) {
            return ma[res].dup(ma, res, objType);
        }
        StringBuilder err = new StringBuilder("No suitable method ")
            .append(name).append('(');
        for(int i = 0; i < args.length; ++i) {
            if(i != 0) {
                err.append(", ");
            }
            err.append(args[i].type);
        }
        err.append(") found in ").append(dottedName());
        boolean fst = true;
        for(int i = ma.length; --i >= 0; ) {
            if(!Objects.equals(ma[i].name, name)) {
                continue;
            }
            if(fst) {
                err.append("\nMethods named ").append(name).append(':');
                fst = false;
            }
            err.append("\n    ").append(ma[i]);
        }
        throw new CompileException(n, err.toString());
    }

    JavaType resolve(Node where) {
        try {
            resolve();
        } catch(JavaClassNotFoundException ex) {
            throw new CompileException(where, ex);
        }
        return this;
    }

    public JavaType dup() {
        if(!resolved) {
            throw new IllegalStateException("Cannot clone unresolved class");
        }
        try {
            return (JavaType)clone();
        } catch(CloneNotSupportedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public String str() {
        switch(description.charAt(0)) {
            case 'Z':
                return "boolean";
            case 'B':
                return "byte";
            case 'C':
                return "char";
            case 'D':
                return "double";
            case 'F':
                return "float";
            case 'I':
                return "int";
            case 'J':
                return "long";
            case 'S':
                return "short";
            case 'V':
                return "void";
            case 'L':
                return "~".concat(dottedName());
        }
        return "~".concat(description);
    }

    public static class Field {
        public int access;
        public String name;
        public CType type;
        public CType classType;
        String className;
        public Object constValue;

        public Field(String name, int access,
                     String className, CType type) {
            this.access = access;
            this.type = type;
            this.name = name;
            this.className = className;
        }

        public CType convertedType() {
            return convertValueType(type);
        }

        void check(Node where, String packageName) {
            classType.javaType.checkPackage(where, packageName);
            if((access & classType.javaType.publicMask) == 0) {
                checkPackage(where, packageName, className, "field", name);
            }
        }
    }

    public static class Method {
        public int access;
        public String name;
        public CType[] arguments;
        public CType returnType;
        public CType classType;
        public String className; // name of the class the method actually belongs to
        public String sig;
        protected String descr;

        Method dup(Method[] arr, int n, CType classType) {
            if(classType == this.classType ||
                className.equals(classType.javaType.className())) {
                this.classType = classType;
                return this;
            }
            Method m = new Method();
            m.access = access;
            m.name = name;
            m.arguments = arguments;
            m.returnType = returnType;
            m.classType = classType;
            m.className = className;
            m.sig = sig;
            m.descr = descr;
            arr[n] = m;
            return m;
        }

        Method check(Node where, String packageName, int extraMask) {
            classType.javaType.checkPackage(where, packageName);
            if((access & (classType.javaType.publicMask | extraMask)) == 0) {
                checkPackage(where, packageName, className, "method", name);
            }
            return this;
        }

        public String toString() {
            StringBuilder s =
                new StringBuilder(returnType.type == RiaType.UNIT
                    ? "void" : returnType.toString());
            s.append(' ');
            s.append(name);
            s.append('(');
            for(int i = 0; i < arguments.length; ++i) {
                if(i != 0) {
                    s.append(", ");
                }
                s.append(arguments[i]);
            }
            s.append(")");
            return s.toString();
        }

        public CType convertedReturnType() {
            return convertValueType(returnType);
        }

        String argDescr(int arg) {
            return descriptionOf(arguments[arg]);
        }

        public String descr(String extra) {
            if(descr != null) {
                return descr;
            }
            StringBuilder result = new StringBuilder("(");
            for(int i = 0; i < arguments.length; ++i) {
                result.append(argDescr(i));
            }
            if(extra != null) {
                result.append(extra);
            }
            result.append(')');
            if(returnType.type == RiaType.UNIT) {
                result.append('V');
            } else {
                result.append(descriptionOf(returnType));
            }
            return descr = result.toString();
        }
    }
}
