package ria.lang.compiler;

import ria.lang.compiler.code.BindRef;
import ria.lang.compiler.code.BoolOpFun;
import ria.lang.compiler.code.Closure;
import ria.lang.compiler.code.CompareFun;
import ria.lang.compiler.code.StrOp;
import ria.lang.compiler.nodes.Node;
import ria.lang.compiler.nodes.TypeDef;
import ria.lang.compiler.nodes.TypeNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RiaType {

    public static final int VAR = 0;
    public static final int UNIT = 1;
    public static final int STR = 2;
    public static final int NUM = 3;
    public static final int BOOL = 4;
    public static final int CHAR = 5;
    public static final int NONE = 6;
    public static final int LIST_MARKER = 7;
    public static final int MAP_MARKER = 8;
    public static final int FUN = 9; // a -> b
    public static final int MAP = 10; // value, index, (LIST | MAP)
    public static final int STRUCT = 11;
    public static final int VARIANT = 12;
    public static final int JAVA = 13;
    public static final int JAVA_ARRAY = 14;
    public static final int JAVA_GENERIC = 15;

    public static final int FIELD_NON_POLYMORPHIC = 1;
    public static final int FIELD_MUTABLE = 2;
    public static final int RESTRICT_ALL = 4;
    public static final int RESTRICT_POLY = 8;
    public static final int STRUCT_VAR = 16;

    public static final CType[] NO_PARAM = {};
    public static final CType UNIT_TYPE = new CType(UNIT, NO_PARAM);
    public static final CType NUM_TYPE = new CType(NUM, NO_PARAM);
    public static final CType STR_TYPE = new CType(STR, NO_PARAM);
    public static final CType BOOL_TYPE = new CType(BOOL, NO_PARAM);
    public static final CType CHAR_TYPE = new CType(CHAR, NO_PARAM);
    public static final CType NO_TYPE = new CType(NONE, NO_PARAM);
    public static final CType LIST_TYPE = new CType(LIST_MARKER, NO_PARAM);
    public static final CType A = new CType(1);
    public static final CType B = new CType(1);
    public static final CType C = new CType(1);
    public static final CType EQ_TYPE = fun2Arg(A, A, BOOL_TYPE);
    public static final CType NUMOP_TYPE = fun2Arg(NUM_TYPE, NUM_TYPE, NUM_TYPE);
    public static final CType BOOLOP_TYPE = fun2Arg(BOOL_TYPE, BOOL_TYPE, BOOL_TYPE);
    public static final CType A_B_LIST_TYPE = new CType(MAP, new CType[] {A, B, LIST_TYPE});
    public static final CType NUM_LIST_TYPE = new CType(MAP, new CType[] {NUM_TYPE, B, LIST_TYPE});
    public static final CType A_B_C_MAP_TYPE = new CType(MAP, new CType[] {B, A, C});
    public static final CType A_LIST_TYPE = new CType(MAP, new CType[] {A, NO_TYPE, LIST_TYPE});
    public static final CType C_LIST_TYPE = new CType(MAP, new CType[] {C, NO_TYPE, LIST_TYPE});
    public static final CType STRING_ARRAY = new CType(MAP, new CType[] {STR_TYPE, NUM_TYPE, LIST_TYPE});
    public static final CType CONS_TYPE = fun2Arg(A, A_B_LIST_TYPE, A_LIST_TYPE);
    public static final CType LAZYCONS_TYPE = fun2Arg(A, fun(UNIT_TYPE, A_B_LIST_TYPE), A_LIST_TYPE);
    public static final CType A_TO_BOOL = fun(A, BOOL_TYPE);
    public static final CType LIST_TO_A = fun(A_B_LIST_TYPE, A);
    public static final CType MAP_TO_BOOL = fun(A_B_C_MAP_TYPE, BOOL_TYPE);
    public static final CType MAP_TO_NUM = fun(A_B_C_MAP_TYPE, NUM_TYPE);
    public static final CType LIST_TO_LIST = fun(A_B_LIST_TYPE, A_LIST_TYPE);
    public static final CType IN_TYPE = fun2Arg(A, A_B_C_MAP_TYPE, BOOL_TYPE);
    public static final CType COMPOSE_TYPE = fun2Arg(fun(B, C), fun(A, B), fun(A, C));
    public static final CType BOOL_TO_BOOL = fun(BOOL_TYPE, BOOL_TYPE);
    public static final CType NUM_TO_NUM = fun(NUM_TYPE, NUM_TYPE);
    public static final CType STR_TO_NUM_TO_STR = fun2Arg(STR_TYPE, NUM_TYPE, STR_TYPE);
    public static final CType FOR_TYPE = fun2Arg(A_B_LIST_TYPE, fun(A, UNIT_TYPE), UNIT_TYPE);
    public static final CType FOREACH_TYPE = fun2Arg(fun(A, UNIT_TYPE), A_B_LIST_TYPE, UNIT_TYPE);
    public static final CType STR2_PRED_TYPE = fun2Arg(STR_TYPE, STR_TYPE, BOOL_TYPE);
    public static final CType SYNCHRONIZED_TYPE = fun2Arg(A, fun(UNIT_TYPE, B), B);
    public static final CType CLASS_TYPE = new CType("Ljava/lang/Class;");
    public static final CType OBJECT_TYPE = new CType("Ljava/lang/Object;");
    public static final CType WITH_EXIT_TYPE = fun(fun(fun(A, B), A), A);
    public static final CType THROW_TYPE = fun(new CType("Ljava/lang/Throwable;"), A);
    public static final CType MAP_TYPE = new CType(MAP_MARKER, NO_PARAM);
    // TODO: Check if we still require this
    public static final CType A_B_MAP_TYPE = new CType(MAP, new CType[] {B, A, MAP_TYPE});
    public static final CType ORDERED = orderedVar(1);
    public static final CType COMPARE_TYPE = fun2Arg(ORDERED, ORDERED, BOOL_TYPE);

    static final int PRIMITIVE_END = 8;
    static final int OPAQUE_TYPES = 0x10000;

    static final int FL_ORDERED_REQUIRED = 1;
    static final int FL_TAINTED_VAR = 2;
    static final int FL_AMBIGUOUS_OPAQUE = 4;
    static final int FL_ANY_CASE = 8;
    static final int FL_FLEX_TYPEDEF = 0x10;
    static final int FL_ERROR_IS_HERE = 0x100;
    static final int FL_ANY_PATTERN = 0x4000;
    static final int FL_PARTIAL_PATTERN = 0x8000;

    static final CType[] PRIMITIVES = {
        null, UNIT_TYPE, STR_TYPE, NUM_TYPE, BOOL_TYPE, CHAR_TYPE, NO_TYPE, LIST_TYPE, MAP_TYPE};

    static final String[] TYPE_NAMES = {
        "var", "()", "string", "number", "boolean", "char",
        "none", "list", "hash", "fun", "list", "struct", "variant", "object"
    };

    static final JavaType COMPARABLE = JavaType.fromDescription("Ljava/lang/Comparable;");

    static final Scope ROOT_SCOPE;
    static final Scope ROOT_SCOPE_SYS;

    private static final int RESTRICT_PROTECT = 1;
    private static final int RESTRICT_CONTRA = 2;

    private static final StructVar DENY = new StructVar(1, null);

    // Initialise the environment
    static {
        Scope scope = bindImport("String", "java/lang/String", null);
        scope = bindImport("Double", "java/lang/Double", scope);
        scope = bindImport("Long", "java/lang/Long", scope);
        scope = bindImport("Integer", "java/lang/Integer", scope);
        scope = bindImport("Boolean", "java/lang/Boolean", scope);
        scope = bindImport("Object", "java/lang/Object", scope);
        scope = bindImport("Math", "java/lang/Math", scope);
        scope = bindImport("IllegalArgumentException", "java/lang/IllegalArgumentException", scope);
        scope = bindImport("NumberFormatException", "java/lang/NumberFormatException", scope);
        scope = bindImport("RuntimeException", "java/lang/RuntimeException", scope);
        scope = bindImport("Exception", "java/lang/Exception", scope);
        scope = bindImport("NoSuchKey", "ria/lang/NoSuchKeyException", scope);
        scope = bindImport("Failure", "ria/lang/FailureException", scope);
        scope = bindImport("EmptyArray", "ria/lang/EmptyArrayException", scope);
        scope = bindRegex("matchAll", "ria/lang/MatchAll",
            fun2Arg(STR_TYPE, fun(STRING_ARRAY, A), fun2Arg(fun(STR_TYPE, A), STR_TYPE, A_LIST_TYPE)), scope);
        scope = bindRegex("substAll", "ria/lang/SubstAll",
            fun2Arg(STR_TYPE, STR_TYPE, fun(STR_TYPE, STR_TYPE)), scope);
        scope = bindRegex("like", "ria/lang/Like",
            fun2Arg(STR_TYPE, STR_TYPE, fun(UNIT_TYPE, STRING_ARRAY)), scope);
        scope = bindRegex("strSplit", "ria/lang/StringSplit",
            fun2Arg(STR_TYPE, STR_TYPE, STRING_ARRAY), scope);
        scope = bindStr("strLastIndexOf'",
            fun2Arg(STR_TYPE, STR_TYPE, NUM_TYPE),
            "lastIndexOf", "(Ljava/lang/String;)I", scope);
        scope = bindStr("strLastIndexOf",
            fun2Arg(STR_TYPE, STR_TYPE, fun(NUM_TYPE, NUM_TYPE)),
            "lastIndexOf", "(Ljava/lang/String;I)I", scope);
        scope = bindStr("strIndexOf",
            fun2Arg(STR_TYPE, STR_TYPE, fun(NUM_TYPE, NUM_TYPE)),
            "indexOf", "(Ljava/lang/String;I)I", scope);
        scope = bindStr("strEnds?", fun2Arg(STR_TYPE, STR_TYPE, BOOL_TYPE),
            "endsWith", "(Ljava/lang/String;)Z", scope);
        scope = bindStr("strStarts?", fun2Arg(STR_TYPE, STR_TYPE, BOOL_TYPE),
            "startsWith", "(Ljava/lang/String;)Z", scope);
        scope = bindStr("strRight", fun2Arg(STR_TYPE, NUM_TYPE, STR_TYPE),
            "substring", "(I)Ljava/lang/String;", scope);
        scope = bindStr("strSlice",
            fun2Arg(STR_TYPE, NUM_TYPE, fun(NUM_TYPE, STR_TYPE)), "substring", "(II)Ljava/lang/String;", scope);
        scope = bindStr("strTrim",
            fun(STR_TYPE, STR_TYPE), "trim", "()Ljava/lang/String;", scope);
        scope = bindStr("strLower",
            fun(STR_TYPE, STR_TYPE), "toLowerCase", "()Ljava/lang/String;", scope);
        scope = bindStr("strUpper",
            fun(STR_TYPE, STR_TYPE), "toUpperCase", "()Ljava/lang/String;", scope);
        scope = bindStr("strLength",
            fun(STR_TYPE, NUM_TYPE), "length", "()I", scope);
        scope = bindScope("strChar", new Intrinsic(16), scope);
        scope = bindScope("undef_str", new Intrinsic(23), scope);
        scope = bindScope("negate", new Intrinsic(20), scope);
        scope = bindScope("true", new Intrinsic(19), scope);
        scope = bindScope("false", new Intrinsic(18), scope);
        scope = bindScope("undef_bool", new Intrinsic(17), scope);
        scope = bindScope("or", new BoolOpFun(true), scope);
        scope = bindScope("and", new BoolOpFun(false), scope);
        scope = bindScope("not", new Intrinsic(15), scope);
        scope = bindScope("!~", new Intrinsic(14), scope);
        scope = bindScope("=~", new Intrinsic(13), scope);
        scope = bindArith("xor", "xor", scope);
        scope = bindArith("b_or", "or", scope);
        scope = bindArith("b_and", "and", scope);
        scope = bindArith("shr", "shr", scope);
        scope = bindArith("shl", "shl", scope);
        scope = bindArith("div", "intDiv", scope);
        scope = bindArith("%", "rem", scope);
        scope = bindArith("/", "div", scope);
        scope = bindArith("*", "mul", scope);
        scope = bindArith("-", "sub", scope);
        scope = bindArith("+", "add", scope);
        scope = bindPoly("throw", WITH_EXIT_TYPE, new Intrinsic(26), scope);
        scope = bindPoly("length", MAP_TO_NUM, new Intrinsic(25), scope);
        scope = bindPoly("withExit", WITH_EXIT_TYPE, new Intrinsic(24), scope);
        scope = bindPoly("latch", SYNCHRONIZED_TYPE, new Intrinsic(7), scope);
        scope = bindPoly("tail", LIST_TO_LIST, new Intrinsic(12), scope);
        scope = bindPoly("head", LIST_TO_A, new Intrinsic(11), scope);
        scope = bindPoly("same?", EQ_TYPE, new Intrinsic(21), scope);
        scope = bindPoly("empty?", MAP_TO_BOOL, new Intrinsic(10), scope);
        scope = bindPoly("defined?", A_TO_BOOL, new Intrinsic(9), scope);
        scope = bindPoly("nullptr?", A_TO_BOOL, new Intrinsic(8), scope);
        scope = bindPoly("for", FOR_TYPE, new Intrinsic(5), scope);
        scope = bindPoly("foreach", FOREACH_TYPE, new Intrinsic(27), scope);
        scope = bindPoly(":.", LAZYCONS_TYPE, new Intrinsic(4), scope);
        scope = bindPoly(":+", CONS_TYPE, new Intrinsic(3), scope);
        scope = bindPoly("in", IN_TYPE, new Intrinsic(2), scope);
        scope = bindPoly(".", COMPOSE_TYPE, new Intrinsic(6), scope);
        scope = bindCompare(">=", COMPARE_TYPE, CompareFun.COND_GE, scope);
        scope = bindCompare(">", COMPARE_TYPE, CompareFun.COND_GT, scope);
        scope = bindCompare("<=", COMPARE_TYPE, CompareFun.COND_LE, scope);
        scope = bindCompare("<", COMPARE_TYPE, CompareFun.COND_LT, scope);
        scope = bindCompare("!=", EQ_TYPE, CompareFun.COND_NOT, scope);// equals is 0 for false
        scope = bindCompare("==", EQ_TYPE, CompareFun.COND_EQ, scope);// equals is 0 for false

        ROOT_SCOPE = scope;

        scope = bindImport("Class", "java/lang/Class", scope);
        scope = bindImport("System", "java/lang/System", scope);

        ROOT_SCOPE_SYS = scope;
    }

    static Scope bindScope(String name, Binder binder, Scope scope) {
        return new Scope(scope, name, binder);
    }

    static Scope bindCompare(String op, CType type, int code, Scope scope) {
        return bindPoly(op, type, new Compare(type, code, op), scope);
    }

    static Scope bindArith(String op, String method, Scope scope) {
        return bindScope(op, new ArithOp(op, method, NUMOP_TYPE), scope);
    }

    static Scope bindStr(String name, CType type, String method, String sig, Scope scope) {
        return bindScope(name, new StrOp(name, method, sig, type), scope);
    }

    static Scope bindRegex(String name, String impl, CType type, Scope scope) {
        return bindPoly(name, type, new Regex(name, impl, type), scope);
    }

    static Scope bindImport(String name, String className, Scope scope) {
        scope = new Scope(scope, name, null);
        scope.importClass = new ClassBinding(new CType('L' + className + ';'));
        return scope;
    }

    static CType fun(CType a, CType res) {
        return new CType(FUN, new CType[] {a, res});
    }

    static CType fun2Arg(CType a, CType b, CType res) {
        return new CType(FUN,
            new CType[] {a, new CType(FUN, new CType[] {b, res})});
    }

    static CType mutableFieldRef(CType src) {
        CType t = new CType(src.depth);
        t.ref = src.ref;
        t.flags = src.flags;
        t.field = FIELD_MUTABLE;
        return t;
    }

    static CType fieldRef(int depth, CType ref, int kind) {
        CType t = new CType(depth);
        t.ref = ref.deref();
        t.field = kind;
        t.doc = ref.doc;
        return t;
    }

    static CType orderedVar(int maxDepth) {
        CType type = new CType(maxDepth);
        type.flags = FL_ORDERED_REQUIRED;
        return type;
    }

    static void limitDepth(CType type, int maxDepth, int setFlag) {
        type = type.deref();
        if(type.type != VAR) {
            if(!type.seen) {
                type.seen = true;
                for(int i = type.param.length; --i >= 0; ) {
                    limitDepth(type.param[i], maxDepth, setFlag);
                }
                type.seen = false;
            }
        } else {
            if(type.depth > maxDepth) {
                type.depth = maxDepth;
            }
            type.flags |= setFlag;
        }
    }

    static void mismatch(CType a, CType b) throws TypeException {
        throw new TypeException(a, b);
    }

    static void finalizeStruct(CType partial, CType src) throws TypeException {
        if(src.allowedMembers == null || partial.requiredMembers == null) {
            return; // nothing to check
        }
        String current = null;
        try {
            for(Map.Entry<String, CType> member : partial.requiredMembers.entrySet()) {
                String name = member.getKey();
                CType ff = src.allowedMembers.get(name);
                if(ff == null) {
                    if((partial.flags & FL_ANY_CASE) != 0) {
                        continue;
                    }
                    throw new TypeException(src, " => ", partial, " (member missing: " + name + ")");
                }
                CType partField = member.getValue();
                if(partField.field == FIELD_MUTABLE &&
                    ff.field != FIELD_MUTABLE) {
                    throw new TypeException("Field '" + name + "' constness mismatch: " + src + " => " + partial);
                }
                current = name;
                unify(partField, ff);
                current = null;
            }
        } catch(TypeException ex) {
            if(current != null) {
                ex.trace.add(current);
                ex.trace.add(partial);
                ex.trace.add(src);
            }
            throw ex;
        }
    }

    // Unifies the members of the types
    static void unifyMembers(CType a, CType b) throws TypeException {
        CType oldRef = b.ref;
        String currentField = null;
        try {
            b.ref = a; // just fake ref now to avoid cycles...
            Map<String, CType> ff;
            if(((a.flags | b.flags) & FL_FLEX_TYPEDEF) != 0) {
                int x = (a.allowedMembers != null ? 1 : 0) |
                    (a.requiredMembers != null ? 2 : 0) |
                    (b.allowedMembers != null ? 4 : 0) |
                    (b.requiredMembers != null ? 8 : 0);
                if(x == 6 || x == 9) { // 0110 or 1001
                    CType t = (a.flags & FL_FLEX_TYPEDEF) != 0 ? a : b;
                    Map<String, CType> tmp = t.allowedMembers;
                    t.allowedMembers = t.requiredMembers;
                    t.requiredMembers = tmp;
                    t.flags &= ~FL_FLEX_TYPEDEF; // flip only once.
                }
            }
            // don't be smart
            a.flags &= ~FL_FLEX_TYPEDEF;
            if(((a.flags ^ b.flags) & FL_ORDERED_REQUIRED) != 0) {
                // VARIANT types are sometimes ordered.
                // when all their variant parameters are ordered types.
                // ensure that if a or b is ordered then the other one is too
                if((a.flags & FL_ORDERED_REQUIRED) != 0) {
                    requireOrdered(b);
                } else {
                    requireOrdered(a);
                }
            }

            // Check the allowed members and ensure that both are set
            if(a.allowedMembers == null) {
                ff = b.allowedMembers;
            } else if(b.allowedMembers == null) {
                ff = a.allowedMembers;
            } else {
                // unify final members
                ff = new HashMap<>(a.allowedMembers);
                for(Iterator<Map.Entry<String, CType>> i = ff.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry<String, CType> entry = i.next();
                    currentField = entry.getKey();
                    CType f = b.allowedMembers.get(currentField);
                    if(f != null) {
                        CType t = entry.getValue();
                        unify(f, t);
                        // constness spreads
                        if(t.field != f.field) {
                            if(t.field == 0) {
                                entry.setValue(t = f);
                            }
                            t.field = FIELD_NON_POLYMORPHIC;
                        }
                    } else {
                        i.remove();
                    }
                }
                currentField = null;
                if(ff.isEmpty()) {
                    mismatch(a, b);
                }
            }
            finalizeStruct(a, b);
            finalizeStruct(b, a);

            if(ff != null && (b.flags & FL_ANY_CASE) != 0) {
                if((a.flags & FL_ANY_CASE) != 0) {
                    a.requiredMembers = null;
                }
            } else if(a.requiredMembers == null || (ff != null && (a.flags & FL_ANY_CASE) != 0)) {
                a.requiredMembers = b.requiredMembers;
            } else if(b.requiredMembers != null) {
                // join partial members
                for(Map.Entry<String, CType> anAa : a.requiredMembers.entrySet()) {
                    currentField = anAa.getKey();
                    CType f = b.requiredMembers.get(currentField);
                    if(f != null) {
                        unify(anAa.getValue(), f);
                        // mutability spreads
                        if(f.field >= FIELD_NON_POLYMORPHIC) {
                            anAa.setValue(f);
                        }
                    }
                }
                currentField = null;
                a.requiredMembers.putAll(b.requiredMembers);
            }
            a.allowedMembers = ff;
            a.flags &= b.flags | ~(FL_ANY_CASE | FL_FLEX_TYPEDEF);
            if(ff == null) {
                ff = a.requiredMembers;
            } else if(a.requiredMembers != null) {
                ff = new HashMap<>(ff);
                ff.putAll(a.requiredMembers);
            }
            unify(a.param[0], b.param[0]);
            structParam(a, ff, a.param[0].deref());
            b.type = VAR;
            b.ref = a;
            if((a.param[0].flags & FL_TAINTED_VAR) != 0) {
                limitDepth(a, a.param[0].depth, FL_TAINTED_VAR);
            }
        } catch(TypeException ex) {
            b.ref = oldRef;
            if(currentField != null) {
                ex.trace.add(currentField);
                ex.trace.add(a);
                ex.trace.add(b);
            }
            throw ex;
        }
    }

    static void structParam(CType st, Map<String, CType> values, CType depth) {
        if(depth.type != VAR || depth.ref != null) {
            throw new IllegalStateException("non-free var at struct depth: " + depth);
        }
        CType[] a = new CType[values.size() + 1];
        a[0] = depth;
        Iterator<CType> i = values.values().iterator();
        for(int j = 1; i.hasNext(); ++j) {
            a[j] = i.next();
        }
        st.param = a;
    }

    static void unifyJava(CType jt, CType t) throws TypeException {
        String descr = jt.javaType.description;
        if(t.type != JAVA) {
            // Check for void == UNIT
            if(t.type == UNIT && Objects.equals(descr, "V")) {
                return;
            }
            mismatch(jt, t);
        }
        if(!Objects.equals(descr, t.javaType.description)) {
            mismatch(jt, t);
        }
    }

    static void requireOrdered(CType type) throws TypeException {
        switch(type.type) {
            case VARIANT:
                if((type.flags & FL_ORDERED_REQUIRED) == 0) {
                    if(type.requiredMembers != null) {
                        for(CType o : type.requiredMembers.values()) {
                            requireOrdered(o);
                        }
                    }
                    if(type.allowedMembers != null) {
                        for(CType o : type.allowedMembers.values()) {
                            requireOrdered(o);
                        }
                        type.flags |= FL_ORDERED_REQUIRED;
                    }
                }
                return;
            case MAP:
                requireOrdered(type.param[2]);
                requireOrdered(type.param[0]);
                return;
            case VAR:
                if(type.ref != null) {
                    requireOrdered(type.ref);
                } else {
                    type.flags |= FL_ORDERED_REQUIRED;
                }
            case NUM:
            case STR:
            case LIST_MARKER:
                return;
            case JAVA:
                try {
                    if(COMPARABLE.isAssignable(type.javaType) >= 0) {
                        return;
                    }
                } catch(JavaClassNotFoundException ex) {
                    throw new TypeException("Unknown class: " + ex.getMessage());
                }
        }
        TypeException ex = new TypeException(type + " is not an ordered type");
        ex.special = true;
        throw ex;
    }

    static void occursCheck(CType type, CType var) throws TypeException {
        type = type.deref();
        if(type == var) {
            TypeException ex = new TypeException("Cyclic types are not allowed");
            ex.special = true;
            throw ex;
        }
        if(type.param != null && type.type != VARIANT && type.type != STRUCT) {
            for(int i = type.param.length; --i >= 0; ) {
                occursCheck(type.param[i], var);
            }
        }
    }

    static void unifyToVar(CType var, CType from) throws TypeException {
        occursCheck(from, var);
        if((var.flags & FL_ORDERED_REQUIRED) != 0) {
            requireOrdered(from);
        }
        limitDepth(from, var.depth, var.flags & FL_TAINTED_VAR);
        var.ref = from;
    }

    // Unify two types
    static void unify(CType a, CType b) throws TypeException {
        a = a.deref();
        b = b.deref();

        // If they are already the same type, then we are done
        if(a == b) {
            return;
        }

        // Check for Variant types
        if(a.type == VAR) {
            unifyToVar(a, b);
            return;
        }
        if(b.type == VAR) {
            unifyToVar(b, a);
            return;
        }

        // If they are not the same type, then we check for opaque types
        if(a.type != b.type) {
            CType opaque = null;
            if(a.type >= OPAQUE_TYPES && (a.flags & FL_AMBIGUOUS_OPAQUE) != 0) {
                opaque = a;
            } else if(b.type >= OPAQUE_TYPES && (b.flags & FL_AMBIGUOUS_OPAQUE) != 0) {
                opaque = b;
            }
            if(opaque != null) {
                opaque.ref = opaque.allowedMembers.values().toArray(new CType[0])[0];
                opaque.type = 0;
                unify(a, b);
                return;
            }
        }

        // Now unify Java types
        if(a.type == JAVA) {
            unifyJava(a, b);
        } else if(b.type == JAVA) {
            unifyJava(b, a);
        } else if(a.type != b.type) {
            // We cannot unify these types
            mismatch(a, b);
        } else if(a.type == STRUCT || a.type == VARIANT) {
            // Unify structs and variants
            unifyMembers(a, b);
        } else if(a.type == MAP &&
            (a.param[1].type ^ b.param[1].type) == (NUM ^ NONE) &&
            (a.param[1].type == NONE || b.param[1].type == NONE)) {
            // We have a map type that cannot be unified
            mismatch(a, b);
        } else {
            for(int i = 0, cnt = a.param.length; i < cnt; ++i) {
                // Attempt to unify all type parameters
                unify(a.param[i], b.param[i]);
            }
            if(a.type >= OPAQUE_TYPES && (a.flags & b.flags & FL_AMBIGUOUS_OPAQUE) == 0) {
                // Remove the ambiguous flag on the types, as we are resolved now
                a.flags &= ~FL_AMBIGUOUS_OPAQUE;
                b.flags &= ~FL_AMBIGUOUS_OPAQUE;
            }
        }
    }

    static void unify(CType a, CType b, Node where, Scope scope, CType param1, CType param2, String error) {
        try {
            unify(a, b);
        } catch(TypeException ex) {
            throw new CompileException(where, scope, param1, param2, error, ex);
        }
    }

    static void unify(CType a, CType b, Node where, Scope scope, String error) {
        unify(a, b, where, scope, a, b, error);
    }

    // Attempt to merge types
    static CType mergeOrUnify(CType to, CType val) throws TypeException {
        CType t = JavaType.mergeTypes(to, val);
        if(t != null) {
            return t;
        }
        unify(to, val);
        return to;
    }

    static Map<String, CType> copyTypeMap(Map<String, CType> types, Map<CType, CType> free, Map<CType, CType> known) {
        Map<String, CType> result = new HashMap<>(types.size());
        for(Map.Entry<String, CType> o : types.entrySet()) {
            CType t = o.getValue();
            CType nt = copyType(t, free, known);
            // looks like a hack, but fixing here avoids unnecessary refs
            if(t.field != nt.field) {
                if(t.field != 0) {
                    CType tmp = new CType(0);
                    tmp.ref = nt;
                    nt = tmp;
                }
                nt.field = t.field;
                nt.flags = t.flags;
            }
            result.put(o.getKey(), nt);
        }
        return result;
    }

    // free should be given null only in that special case,
    // when it is desirable to copy non-polymorphic structures.
    // Only known such case currently is in the opaqueCast function.
    static CType copyType(CType type_, Map<CType, CType> free, Map<CType, CType> known) {
        CType res, type = type_.deref();
        if(type.type == VAR) {
            return free != null && (res = free.get(type)) != null
                ? res : type;
        }
        if(type.param.length == 0 && type.type < OPAQUE_TYPES) {
            return type_;
        }
        CType copy = known.get(type);
        if(copy != null) {
            return copy;
        }
        /* No structure without polymorphic flag variable shouldn't be copied.
         * The getFreeVar should ensure that any variable reachable through
         * such structure isn't free either.
         */
        if((type.type == STRUCT || type.type == VARIANT) &&
            free != null && !free.containsKey(type.param[0])) {
            return type;
        }
        CType[] param = new CType[type.param.length];
        copy = new CType(type.type, param);
        copy.doc = type_;
        res = copy;
        if(type_.field >= FIELD_NON_POLYMORPHIC) {
            res = mutableFieldRef(type_);
            res.field = type_.field;
            res.ref = copy;
        }
        known.put(type, res);
        for(int i = param.length; --i >= 0; ) {
            param[i] = copyType(type.param[i], free, known);
        }
        if(type.requiredMembers != null) {
            copy.flags = type.flags & (FL_ANY_CASE | FL_FLEX_TYPEDEF);
            copy.requiredMembers = copyTypeMap(type.requiredMembers, free, known);
        }
        if(type.allowedMembers != null) {
            copy.flags |= type.flags & FL_FLEX_TYPEDEF;
            copy.allowedMembers = copyTypeMap(type.allowedMembers, free, known);
        }
        return res;
    }

    static Map<CType, CType> createFreeVars(CType[] freeTypes, int depth) {
        HashMap<CType, CType> vars = new HashMap<>(freeTypes.length);
        for(int i = freeTypes.length; --i >= 0; ) {
            CType free = freeTypes[i];
            CType t = new CType(depth);
            t.flags = free.flags;
            t.field = free.field;
            vars.put(free, t);
        }
        return vars;
    }

    private static BindRef resolveRef(String sym, Node where,
                                      Scope scope, Scope[] r) {
        for(; scope != null; scope = scope.outer) {
            if(Objects.equals(scope.name, sym) && scope.binder != null) {
                r[0] = scope;
                return scope.binder.getRef(where.line);
            }
            if(scope.closure != null) {
                return scope.closure.refProxy(
                    resolveRef(sym, where, scope.outer, r));
            }
        }
        throw new CompileException(where, "Unknown identifier: " + sym);
    }

    static BindRef resolve(String sym, Node where, Scope scope, int depth) {
        Scope[] r = new Scope[1];
        BindRef ref = resolveRef(sym, where, scope, r);
        // We have to copy even polymorph refs with NO free variables,
        // because the structs are picky with their provided/requested member lists.
        if(r[0].free != null && (ref.polymorph || r[0].free.length != 0)) {
            ref = ref.unshare();
            Map<CType, CType> vars = createFreeVars(r[0].free, depth + 1);
            ref.type = copyType(ref.type, vars, new HashMap<>());
        }
        return ref;
    }

    static CType resolveClass(String name, Scope scope, boolean shadow) {
        if(name.indexOf('/') >= 0) {
            return JavaType.typeOfClass(null, name);
        }
        for(; scope != null; scope = scope.outer) {
            if(Objects.equals(scope.name, name)) {
                if(scope.importClass != null) {
                    return scope.importClass.type;
                }
                if(shadow) {
                    break;
                }
            }
        }
        return null;
    }

    static ClassBinding resolveFullClass(String name, Scope scope,
                                         boolean refs, Node checkPerm) {
        String packageName = scope.ctx.packageName;
        CType t;
        if(name.indexOf('/') >= 0) {
            packageName = null;
        } else if(refs) {
            List<Closure> proxies = new ArrayList<>();
            for(Scope s = scope; s != null; s = s.outer) {
                if(Objects.equals(s.name, name) && s.importClass != null) {
                    return s.importClass.dup(proxies);
                }
                if(s.closure != null) {
                    proxies.add(s.closure);
                }
            }
        } else if((t = resolveClass(name, scope, false)) != null) {
            return new ClassBinding(t);
        }
        if(checkPerm != null &&
            (scope.ctx.compiler.globalFlags & Compiler.GF_NO_IMPORT) != 0) {
            throw new CompileException(checkPerm, name + " is not imported");
        }
        return new ClassBinding(JavaType.typeOfClass(packageName, name));
    }

    static CType resolveFullClass(String name, Scope scope) {
        CType t = resolveClass(name, scope, false);
        return t == null ?
            JavaType.typeOfClass(scope.ctx.packageName, name) : t;
    }

    static boolean hasMutableStore(Map<CType, CType> bindVars, CType result, boolean store) {
        if(!result.seen) {
            if(result.field >= FIELD_NON_POLYMORPHIC) {
                store = true;
            }
            CType t = result.deref();
            if(t.type == VAR) {
                return store && bindVars.containsKey(t);
            }
            if(t.type == MAP && t.param[1] != NO_TYPE) {
                store = true;
            }
            result.seen = true;
            for(int i = t.param.length; --i >= 0; ) {
                if(hasMutableStore(bindVars, t.param[i], store || i == 0 && t.type == FUN)) {
                    result.seen = false;
                    return true;
                }
            }
            result.seen = false;
        }
        return false;
    }

    // difference from getFreeVar is that functions don't protect
    static void restrictArg(CType type, int depth, boolean active) {
        if(type.seen) {
            return;
        }
        if(type.field >= FIELD_NON_POLYMORPHIC) {
            active = true; // anything under mutable field is evil
        }
        CType t = type.deref(), k;
        int tt = t.type;
        if(tt != VAR) {
            type.seen = true;
            for(int i = t.param.length; --i >= 0; ) {
                if(i == 1 && !active) {
                    active = tt == MAP && (k = t.param[1].deref()) != NO_TYPE
                        && (k.type != VAR || t.param[2] != LIST_TYPE);
                }
                // array/hash value is in mutable store and evil
                restrictArg(t.param[i], depth, active);
            }
            type.seen = false;
        } else if(active && t.depth >= depth) {
            t.flags |= FL_TAINTED_VAR;
        }
    }

    private static void addFreeVar(Map<CType, RiaType.StructVar> vars, StructVar deps,
                                   CType t, int flags) {
        if((flags & RESTRICT_ALL) != 0) {
            t.flags |= FL_TAINTED_VAR;
            deps = DENY;
        } else if((flags & (RESTRICT_CONTRA | RESTRICT_POLY)) ==
            RESTRICT_CONTRA &&
            (t.flags & FL_TAINTED_VAR) != 0) {
            deps = DENY;
        }
        Object old = vars.put(t, deps);
        // Non-deny struct flag-mutable must always get a new instance
        if(old == null && (flags & STRUCT_VAR) == 0 || deps == DENY) {
            return;
        }
        if(old == DENY) {
            deps = DENY;
        } else {
            deps = new StructVar(0, deps);
            deps.link = (StructVar)old;
        }
        vars.put(t, deps);
    }

    private static void scanFreeVar(Map<CType, RiaType.StructVar> vars, StructVar deps, CType type, int flags, int depth) {
        if(!type.seen) {
            if(type.field >= FIELD_NON_POLYMORPHIC) {
                flags |= (flags & RESTRICT_PROTECT) == 0 ? RESTRICT_ALL : RESTRICT_CONTRA;
            }
            CType t = type.deref();
            int tt = t.type;
            if(tt == STRUCT || tt == VARIANT) {
                type.seen = true;
                scanFreeVar(vars, deps, t.param[0], flags | STRUCT_VAR, depth);
                deps = vars.get(t.param[0].deref());
                for(int i = 1; i < t.param.length; ++i) {
                    scanFreeVar(vars, deps, t.param[i], flags, depth);
                }
                type.seen = false;
            } else if(tt != VAR) {
                if(tt == FUN) {
                    flags |= RESTRICT_PROTECT;
                }
                type.seen = true;
                for(int i = t.param.length; --i >= 0; ) {
                    // array/hash value is in mutable store and evil
                    if(i == 0 && tt == FUN) {
                        flags |= RESTRICT_CONTRA;
                    } else if(i == 1 && tt == MAP && t.param[1].deref() != NO_TYPE) {
                        flags |= (flags & RESTRICT_PROTECT) == 0 ? RESTRICT_ALL : RESTRICT_CONTRA;
                    }
                    scanFreeVar(vars, deps, t.param[i], flags, depth);
                }
                type.seen = false;
            } else if(t.depth > depth) {
                addFreeVar(vars, deps, t, flags);
            } else if((flags & RESTRICT_ALL) != 0 && t.depth == depth) {
                t.flags |= FL_TAINTED_VAR;
            }
        }
    }

    private static boolean purgeNonFree(StructVar var) {
        var.deny = -1; // already seen
        if(var.next != null && var.next.deny != 0 &&
            (var.next.deny > 0 || purgeNonFree(var.next)) ||
            var.link != null && var.link.deny != 0 &&
                (var.link.deny > 0 || purgeNonFree(var.link))) {
            var.deny = 1;
            return true;
        }
        return false;
    }

    static CType[] getFreeVar(Map<CType, RiaType.StructVar> vars, CType type, int flags, int depth) {
        scanFreeVar(vars, null, type, flags, depth);
        if((flags & RESTRICT_ALL) != 0) {
            return NO_PARAM;
        }
        CType[] tv = new CType[vars.size()];
        StructVar[] v = new StructVar[tv.length];
        int n = 0;
        for(Iterator<Map.Entry<CType, StructVar>> i = vars.entrySet().iterator(); i.hasNext(); ++n) {
            Map.Entry<CType, StructVar> e = i.next();
            tv[n] = e.getKey();
            StructVar var = v[n] = e.getValue();
            if(var != null && var.deny == 0 && purgeNonFree(var)) {
                var.deny = 1;
            }
        }
        n = 0;
        for(int i = 0; i < tv.length; ++i) {
            if(v[i] == null || v[i].deny <= 0) {
                tv[n++] = tv[i];
            }
        }
        CType[] result = new CType[n];
        System.arraycopy(tv, 0, result, 0, n);
        return result;
    }

    static void getAllTypeVar(List<CType> vars, List<CType> structs, CType type,
                              boolean freeze) {
        if(type.seen) {
            return;
        }
        CType t = type.deref();
        if(t.type != VAR) {
            type.seen = true;
            int i = -1;
            if(structs != null && (t.type == STRUCT || t.type == VARIANT)) {
                getAllTypeVar(structs, null, t.param[i = 0], false);
                if(freeze) {
                    if(t.allowedMembers == null) {
                        t.allowedMembers = t.requiredMembers;
                    } else {
                        t.requiredMembers = t.allowedMembers;
                    }
                    t.flags &= ~FL_FLEX_TYPEDEF;
                }
            }
            while(++i < t.param.length) {
                getAllTypeVar(vars, structs, t.param[i], freeze);
            }
            type.seen = false;
        } else if(vars.indexOf(t) < 0) {
            vars.add(t);
        }
    }

    static void removeStructs(CType t, Collection vars) {
        if(!t.seen) {
            if(t.type != VAR) {
                int i = 0;
                if(t.type == STRUCT || t.type == VARIANT) {
                    vars.remove(t.param[0].deref());
                    i = 1;
                } else if(t.type == MAP) {
                    // MAP marker type mutable shouldn't really cause
                    // polymorphism restrictions - no real data associated.
                    vars.remove(t.param[2].deref());
                }
                t.seen = true;
                while(i < t.param.length) {
                    removeStructs(t.param[i++], vars);
                }
                t.seen = false;
            } else if(t.ref != null) {
                removeStructs(t.ref, vars);
            }
        }
    }

    static void normalizeFlexType(CType t, boolean covariant) {
        t = t.deref();
        if(t.type != VAR && !t.seen) {
            if((t.flags & FL_FLEX_TYPEDEF) != 0 &&
                (t.type == STRUCT || t.type == VARIANT)) {
                Map<String, CType> members = t.requiredMembers;
                if(t.type == STRUCT ^ members != null ^ covariant &&
                    (members == null || t.allowedMembers == null)) {
                    t.requiredMembers = t.allowedMembers;
                    t.allowedMembers = members;
                }
                t.flags &= ~FL_FLEX_TYPEDEF;
            }
            t.seen = true;
            for(int i = 0; i < t.param.length; ++i) {
                normalizeFlexType(t.param[i],
                    (i == 0 && t.type == FUN) ^ covariant);
            }
            t.seen = false;
        }
    }

    // strip == false -> it instead introduces flex types
    static void stripFlexTypes(CType t, boolean strip) {
        if(t.type != VAR && !t.seen) {
            if(strip) {
                t.flags &= ~FL_FLEX_TYPEDEF;
            } else if((t.type == STRUCT || t.type == VARIANT) &&
                t.requiredMembers == null ^ t.allowedMembers == null) {
                t.flags |= FL_FLEX_TYPEDEF;
            }
            t.seen = true;
            for(int i = 0; i < t.param.length; ++i) {
                stripFlexTypes(t.param[i].deref(), strip);
            }
            t.seen = false;
        }
    }

    static Scope bind(String name, CType valueType, Binder value, int flags, int depth, Scope scope) {
        scope = new Scope(scope, name, value);
        scope.free = getFreeVar(new HashMap<>(), valueType, flags, depth);
        // If structure should be polymorphic,
        // then at least its mutable flag will be in free
        if(scope.free.length == 0) {
            scope.free = null;
        }
        return scope;
    }

    static Scope bindPoly(String name, CType valueType, Binder value, Scope scope) {
        return bind(name, valueType, value, RESTRICT_POLY, 0, scope);
    }

    static CType resolveTypeDef(Scope scope, String name, CType[] param, int depth, TypeNode src, int def) {
        for(; scope != null; scope = scope.outer) {
            CType[] typeDef;
            if(Objects.equals(scope.name, name) && (typeDef = scope.typedef(true)) != null) {
                if(typeDef.length - 1 != param.length) {
                    throw new CompileException(src, "Type " + name + " expects "
                        + (typeDef.length == 2 ? "1 parameter"
                        : (typeDef.length - 1) + " parameters")
                        + ", not " + param.length);
                }
                if(scope.free == null) { // shared typedef
                    if(def >= 0 && def != TypeDef.UNSHARE) {
                        break; // normal typedef may not use shared ones
                    }
                    return typeDef[0];
                }
                if(def == TypeDef.UNSHARE) {
                    break;
                }
                Map<CType, CType> vars = createFreeVars(scope.free, depth);
                for(int i = param.length; --i >= 0; ) {
                    vars.put(typeDef[i], param[i]);
                }
                CType res = copyType(typeDef[param.length], vars, new HashMap<>());
                if(src.exact) {
                    stripFlexTypes(res, true);
                }
                return res;
            }
        }
        throw new CompileException(src, "Unknown type: " + name);
    }

    // Used by as cast to mark opaque types as ambiguous, allowing them
    // to later unify with their hidden (wrapped) type.
    private static void prepareOpaqueCast(CType type, boolean[] known) {
        if(!type.seen) {
            CType t = type.deref();
            if(t.type != VAR) {
                type.seen = true;
                if(t.type >= OPAQUE_TYPES && known[t.type - OPAQUE_TYPES]) {
                    t.flags |= FL_AMBIGUOUS_OPAQUE;
                }
                for(int i = t.param.length; --i >= 0; ) {
                    prepareOpaqueCast(t.param[i], known);
                }
                type.seen = false;
            }
        }
    }

    private static Map<String, CType> opaqueMembers(Map<String, CType> src, Map<String, CType> members) {
        if(src != null) {
            src = new HashMap<>(src);
            for(Map.Entry<String, CType> o : src.entrySet()) {
                CType t = members.get(o.getKey());
                if(t != null) {
                    o.setValue(t);
                }
            }
        }
        return src;
    }

    // Creates a type from opaque where non-opaque parts are replaced with
    // matching ones from the src. The idea is to hide parts of the src
    // that are opaque types in the opaque argument.
    private static CType deriveOpaque(CType src, CType opaque, Map<CType, CType> cache, boolean[] mask) {
        opaque = opaque.deref();
        CType res = cache.get(opaque);
        if(res != null) {
            return res;
        }
        if(opaque.type >= OPAQUE_TYPES && mask[opaque.type - OPAQUE_TYPES]) {
            cache.put(opaque, opaque);
            return opaque;
        }
        CType s, t;
        boolean hasOpaque = false;
        if(opaque.type == STRUCT || opaque.type == VARIANT) {
            res = new CType(src.type, NO_PARAM);
            cache.put(opaque, res);
            Map<String, CType> members = new HashMap<>(opaque.allowedMembers != null
                ? opaque.allowedMembers : opaque.requiredMembers);
            for(Iterator<Map.Entry<String, CType>> i = members.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, CType> e = i.next();
                s = (src.allowedMembers != null ? src.allowedMembers
                    : src.requiredMembers).get(e.getKey());
                if(s == null) {
                    i.remove();
                    continue;
                }
                t = deriveOpaque(s.deref(), e.getValue(), cache, mask);
                if(t != e.getValue()) {
                    e.setValue(t);
                    hasOpaque = true;
                }
            }
            if(hasOpaque) {
                res.allowedMembers = opaqueMembers(src.allowedMembers, members);
                res.requiredMembers = opaqueMembers(src.requiredMembers, members);
                structParam(res, res.requiredMembers != null
                        ? res.requiredMembers : res.allowedMembers,
                    src.param[0].deref());
                if((res.doc = opaque.doc()) == null) {
                    res.doc = src.doc();
                }
                return res;
            }
        } else if(opaque.type > PRIMITIVE_END && opaque.param.length != 0 &&
            opaque.param.length == src.param.length) {
            res = new CType(src.type, new CType[src.param.length]);
            cache.put(opaque, res);
            for(int i = 0; i < res.param.length; ++i) {
                s = src.param[i].deref();
                t = deriveOpaque(s, opaque.param[i], cache, mask);
                res.param[i] = t;
                hasOpaque |= s != t;
            }
            if(hasOpaque) {
                if((res.doc = opaque.doc()) == null) {
                    res.doc = src.doc();
                }
                return res;
            }
        } else {
            res = null;
        }
        if(res != null) {
            res.type = VAR;
            res.ref = src;
            res.param = null;
        }
        cache.put(opaque, src);
        return src;
    }

    static CType opaqueCast(CType from, CType to, Scope scope) throws TypeException {
        if(from.deref().type == VAR) {
            throw new TypeException("Illegal as cast from 'a to non-Java type");
        }
        CType t;
        boolean[] allow_opaque = new boolean[scope.ctx.opaqueTypes.size() + 1];
        for(; scope != null; scope = scope.outer) {
            CType[] typeDef = scope.typedef(false);
            if(typeDef != null) {
                t = typeDef[typeDef.length - 1];
                if(t.type >= OPAQUE_TYPES && t.allowedMembers != null) {
                    allow_opaque[t.type - OPAQUE_TYPES] = true;
                }
            }
        }
        to = to.deref();
        t = copyType(to, null, new HashMap<>());
        prepareOpaqueCast(t, allow_opaque);
        unify(from, t);
        return deriveOpaque(from.deref(), to, new HashMap<>(), allow_opaque);
    }

    static CType withDoc(CType t, String doc) {
        if(doc == null) {
            return t;
        }
        if(t.type > 0 && t.type <= PRIMITIVE_END) {
            CType tmp = t;
            t = new CType(0);
            t.ref = tmp;
        }
        t.doc = doc;
        return t;
    }

    public static class ClassBinding {
        public final CType type;

        ClassBinding(CType classType) {
            this.type = classType;
        }

        public BindRef[] getCaptures() {
            return null;
        }


        ClassBinding dup(List<Closure> proxies) {
            return this;
        }
    }

    static final class ScopeCtx {
        final String packageName;
        final String className;
        final Map<String, CType> opaqueTypes;
        final Compiler compiler;

        ScopeCtx(String className_, Compiler compiler_) {
            packageName = JavaType.packageOfClass(className_);
            className = className_;
            opaqueTypes = compiler_.opaqueTypes;
            compiler = compiler_;
        }
    }

    static class StructVar {
        int deny; // -1 seen, 0 - unseen, 1 - denied
        StructVar link; // concatenated (old) scope
        StructVar next; // outer scope

        StructVar(int deny, StructVar next) {
            this.deny = deny;
            this.next = next;
        }
    }
}
