package ria.lang.compiler;

import org.objectweb.asm.Opcodes;
import java.util.*;

class JavaSource implements Opcodes {
    private static final String[] CHAR_SPOOL = new String[128];
    private static final Map<String, Integer> MODS = new HashMap<>();
    private int p, e;
    private char[] s;
    private String lookahead;
    private String fn;
    private Map<String, JavaNode> classes;
    private Map<String, String> classNames;
    private int line = 1;
    private final String packageName;
    private List<String> imports = new ArrayList<>();
    private String[] importPackages; // list of packages to search on resolve

    private String get(int level) {
        if (lookahead != null) {
            String id = lookahead;
            lookahead = null;
            return id;
        }
        char c, s[] = this.s;
        int p = this.p - 1, e = this.e;
        boolean annotation = false;
        for (;;) {
            // skip whitespace and comments
            while (++p < e && (c = s[p]) >= '\000' && c <= ' ') {
                if (c == '\n' || c == '\r' && p + 1 < e && s[p + 1] != '\n') {
                    ++line;
                }
            }
            if (p + 1 < e && s[p] == '/') {
                if ((c = s[++p]) == '/') {
                    while (++p < e && (c = s[p]) != '\r' && c != '\n') {
                    }
                    --p;
                    continue;
                }
                if (c == '*') {
                    for (++p; ++p < e && ((c = s[p-1]) != '*' || s[p] != '/');) {
                        if (c == '\n' || c == '\r' && s[p] != '\n') {
                            ++line;
                        }
                    }
                    continue;
                }
                --p;
            }
            if (p >= e) {
                this.p = p;
                return null;
            }
            // skip string
            if ((c = s[p]) == '"' || c == '\'') {
                while (++p < e && s[p] != c && s[p] != '\n') {
                    if (s[p] == '\\') {
                        ++p;
                    }
                }
            }
            // skip block
            if (level > 0 && (c != '}' || --level > 0)) {
                if (c == '{') {
                    ++level;
                }
                continue;
            }
            if (level == 0 && (!annotation || c != '(')) {
                if (c != '@') {
                    break;
                }
                while (++p < e && s[p] >= '0') {// skip name
                }
                --p;
                annotation = true;
            } else if (c == '(') {
                --level;
            } else if (c == ')') {
                ++level;
                annotation = false;
            }
        }
        int f = p;
        // get token
        while (p < e && ((c = s[p]) == '_' || c >= 'a' && c <= 'z' ||
              c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c > '~')) {
            ++p;
        }
        if (f == p) {
            for (c = s[p]; ++p < e && c == '.' && s[p] == c;) {
            }
        }
        this.p = p;
        // faster and ensures all operators to be interned
        if (p - f == 1 && (c = s[f]) >= '\000' && c < CHAR_SPOOL.length) {
            return CHAR_SPOOL[c];
        }
        return new String(s, f, p - f);
    }

    void expect(String expect, String id, String at, String name) {
        if (!expect.equals(id)) {
            CompileException e = new CompileException(line, 0, "Expected `" +
                  expect + (id == null ? "EOF" : "', not `" + id + '\'') + at
                  + (name == null ? "" : " (" + name + ")"));
            e.fn = fn;
            throw e;
        }
    }

    private int modifiers() {
        String id;
        Object mod;
        int result = 0;
        while ((mod = MODS.get(id = get(0))) != null) {
            result |= (Integer)mod;
        }
        lookahead = id;
        return result;
    }

    // mode 0 - classname (dotted identifier)
    // mode 1 - variable name (identifier with [])
    // mode 2 - parametric classname (dotted identifier, <>)
    // mode 3 - full type (dotted identifier <> [])
    private String type(int mode) {
        StringBuffer result = null;
        String id = get(0), sep = null;
        if (Objects.equals(id, "{")) {
            return id;
        }
        while (id != null) {
            sep = get(0);
            if (Objects.equals(sep, "<") && mode > 1) {
                int level = 1;
                String x;
                while ((x = get(0)) != null && (!Objects.equals(x, ">") || --level > 0)) {
                    if (Objects.equals(x, "<")) {
                        ++level;
                    }
                }
                sep = get(0);
            }
            if (!Objects.equals(sep, ".") || mode == 1) {
                break;
            }
            if (result == null) {
                result = new StringBuffer(id);
            }
            result.append('/');
            if ((id = get(0)) != null) {
                result.append(id);
            }
        }
        String type = result == null ? id : result.toString();
        if (mode != 0) {
            while (Objects.equals(sep, "[") && mode != 2) {
                expect("]", get(0), " after type name", type);
                type = "[".concat(type);
                sep = get(0);
            }
        }
        lookahead = sep;
        return type;
    }

    private String field(int modifiers, String type, JavaNode target) {
        JavaNode n = null;
        String id = type(1);
        if (id != null && ((modifiers & ACC_PRIVATE) == 0 || Objects.equals(id, "("))) {
            if ("...".equals(id)) {
                type = "[".concat(type);
                if ((id = type(1)) == null) {
                    return null;
                }
            }
            while (id.startsWith("[")) {
                type = "[".concat(type);
                id = id.substring(1);
            }

            if (target == null) {
                return type;
            }
            n = new JavaNode();
            n.modifier = modifiers;
            if (Objects.equals(id, "(")) {
                // We found a constructor
                type = "void";
                n.name = "<init>";
            } else {
                n.name = id;
            }
            n.type = type;
            n.field = target.field;
            target.field = n;
        }
        int method = 0;
        if (Objects.equals(id, "(") || Objects.equals(id = get(0), "(")) {
            List<String> l = new ArrayList<>();
            do {
                modifiers();
                if (Objects.equals(id = type(3), ")")) {
                    break;
                }
                type = field(0, id, null);
                if ((id = get(0)) != null) {
                    l.add(type);
                }
            } while (Objects.equals(id, ","));
            expect(")", id, " after method argument list",
                   n == null ? null : n.name);
            if (n != null) {
                n.argv = l.toArray(new String[0]);
            }
            method = 1;
        } else if (!Objects.equals(id, "=")) {
            return id;
        }
        int level = method;
        while ((id = get(0)) != null && !Objects.equals(id, ";") && (level > 0 || !Objects.equals(id, ","))) {
            if (Objects.equals(id, "{")) {
                get(1);
                if (method != 0) {
                    return ";";
                }
            } else if (Objects.equals(id, "(")) {
                ++level;
            } else if (Objects.equals(id, ")")) {
                --level;
            }
        }
        return id;
    }

    private String readClass(String outer, int modifiers) {
        String id = type(3);
        if ("interface".equals(id)) {
            modifiers |= ACC_INTERFACE | ACC_ABSTRACT;
        } else if (!"class".equals(id)) {
            return id;
        }
        JavaNode cl = new JavaNode();
        cl.source = this;
        cl.modifier = modifiers;
        id = type(2);
        cl.name = outer != null ? outer + '$' + id :
                    packageName.length() != 0 ? packageName + '/' + id : id;
        id = get(0);
        boolean iface_extends = false;
        if ("extends".equals(id)) {
            if ((modifiers & ACC_INTERFACE) == 0) {
                cl.type = type(2);
                id = get(0);
            } else {
                iface_extends = true;
            }
        }
        if (iface_extends || "implements".equals(id)) {
            List<String> impl = new ArrayList<>();
            do {
                impl.add(type(2));
            } while (Objects.equals(id = get(0), ","));
            cl.argv = impl.toArray(new String[0]);
        }
        expect("{", id, " for the class definition body", cl.name);
        while (!Objects.equals(id = readClass(cl.name, modifiers = modifiers()), "}")) {
            if (id == null) {
                return null;
            }
            if (Objects.equals(id, "{")) {
                get(1);
            } else {
                while (!Objects.equals(id, "") && Objects.equals(field(modifiers, id, cl), ",")) {
                }
            }
        }
        classes.put(cl.name, cl);
        return "";
    }

    JavaSource(String sourceName, char[] source, Map<String, JavaNode> classes) {
        fn = sourceName;
        s = source;
        e = source.length;
        this.classes = classes;
        String id = get(0);
        if ("package".equals(id)) {
            packageName = type(0);
            id = get(0);
        } else {
            packageName = "";
        }
        for (; id != null; id = get(0)) {
            if (Objects.equals(id, ";")) {
                continue; // skip toplevel
            }
            if (!"import".equals(id)) {
                break;
            }
            if ("static".equals(id = type(0))) {
                type(0); // ignore import static
            } else if (id != null) {
                imports.add(id);
            }
        }
        lookahead = id;
        while (readClass(null, modifiers()) != null) {
        }
        s = null;
        fn = null;
    }

    /*
     * Java weird inner class implementation means that
     * import w.x.y.z; doesn't tell whether it means really
     * class x.y.z or x.y$z or x$y$z.
     * Only way to find out is to try, which class exists...
     */
    private static String resolveFull(ClassFinder finder, String t, Map<String, String> to) {
        String name = null, full = t;
        char[] cs = t.toCharArray();
        for (int i = cs.length; --i >= 0;) {
            if (cs[i] == '/') {
                if (name == null) {
                    name = t.substring(i + 1, cs.length);
                } else {
                    full = new String(cs);
                }
                if (finder.exists(full)) {
                    full = 'L' + full + ';';
                    if (to != null) {
                        String o = to.put(name, full);
                        if (o != null) // don't override primitives!
                        {
                            to.put(name, o);
                        }
                    }
                    return full;
                }
                cs[i] = '$';
            }
        }
        return null;
    }

    private synchronized void prepareResolve(ClassFinder finder) {
        if (importPackages != null) {
            return; // already done
        }

        classNames = new HashMap<>(JavaType.JAVA_PRIM);
        classNames.remove("number");
        List<String> packages = new ArrayList<>();
        packages.add(packageName.concat("/"));
        packages.add("java/lang/");
        for(String anImport : imports) {
            if(anImport.endsWith("/*")) {
                packages.add(anImport.substring(0, anImport.length() - 1));
            } else {
                resolveFull(finder, anImport, classNames);
            }
        }
        classNames.put("void", "V");
        importPackages = packages.toArray(new String[0]);
        imports = null;
    }

    // Java package imports and inner class naming means that it is impossible
    // to resolve Java type name without knowing what classes really exists
    // (so all source classes must be parsed before attempting any resolving).
    private CType resolve(ClassFinder finder, String type, String[] to, int n) {
        int array = 0, l = type.length();
        while (array < l && type.charAt(array) == '[') {
            ++array;
        }
        int dot = (type = type.substring(array)).indexOf('/');
        String subclass = type, cname = type;
        if (dot > 0) {
            subclass = type.replace('/', '$');
            cname = type.substring(0, dot);
        }
        String res = classNames.get(cname);
        if (res == null && (dot <= 0 || (res = resolveFull(finder, type, null)) == null)) {
            // try to resolve from package imports
            for (int i = 0; res == null && i < importPackages.length; ++i) {
                if (finder.exists(importPackages[i].concat(cname))) {
                    res = importPackages[i].concat(subclass);
                }
            }
            if (res == null) {
                // if we couldn't resolve
                res = dot > 0 ? type : importPackages[0].concat(type);
            }
            res = 'L' + res + ';';
        }
        if (to != null) {
            to[n] = array != 0 || res.length() <= 1 ? type : res.substring(1, res.length() - 1);
            return null;
        }
        CType t = new CType(res);
        while (--array >= 0) {
            t = new CType(RiaType.JAVA_ARRAY, new CType[] { t });
        }
        return t;
    }

    static void loadClass(ClassFinder finder, JavaTypeReader tr, JavaNode n) {
        JavaSource src = n.source;
        src.prepareResolve(finder);
        String cname = n.name;
        String[] superType = { "java/lang/Object" };
        if (n.type != null) {
            src.resolve(finder, n.type, superType, 0);
        }
        String[] interfaces = new String[n.argv == null ? 0 : n.argv.length];
        for (int i = 0; i < interfaces.length; ++i) {
            src.resolve(finder, n.argv[i], interfaces, i);
        }
        tr.visit(0, n.modifier, cname, null, superType[0], interfaces);
        for (JavaNode i = n.field; i != null; i = i.field) {
            int access = i.modifier;
            if ((n.modifier & ACC_INTERFACE) != 0) {
                access |= i.argv == null ? ACC_PUBLIC | ACC_FINAL | ACC_STATIC
                                         : ACC_PUBLIC | ACC_ABSTRACT;
            }
            String name = i.name;
            CType t = src.resolve(finder, i.type, null, 0);
            if (i.argv == null) { // field
                ((access & ACC_STATIC) == 0 ? tr.fields : tr.staticFields)
                    .put(name, new JavaType.Field(name, access, cname, t));
                continue;
            }
            CType[] av = new CType[i.argv.length];
            for (int j = 0; j < av.length; ++j) {
                av[j] = src.resolve(finder, i.argv[j], null, 0);
            }
            JavaType.Method m = new JavaType.Method();
            m.name = name;
            m.access = access;
            m.returnType = t;
            m.arguments = av;
            m.className = cname;
            m.sig = name + m.descr(null);
            if (Objects.equals(name, "<init>")) {
                tr.constructors.add(m);
            } else {
                ((access & ACC_STATIC) == 0 ? tr.methods : tr.staticMethods).add(m);
            }
        }
        if (tr.constructors.size() == 0) {
            // build a default constructor
            tr.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        }
    }

    private static void mod(String name, int value) {
        MODS.put(name, value);
    }

    static {
        char[] x = { ' ' };
        for (short i = 0; i < CHAR_SPOOL.length; ++i) {
            x[0] = (char) i;
            CHAR_SPOOL[i] = new String(x);
        }
        mod("abstract", ACC_ABSTRACT);
        mod("final", ACC_FINAL);
        mod("native", ACC_NATIVE);
        mod("private", ACC_PRIVATE);
        mod("protected", ACC_PROTECTED);
        mod("public", ACC_PUBLIC);
        mod("static", ACC_STATIC);
        mod("strictfp", ACC_STRICT);
        mod("synchronized", ACC_SYNCHRONIZED);
        mod("transient", ACC_TRANSIENT);
        mod("volatile", ACC_VOLATILE);
    }
}
