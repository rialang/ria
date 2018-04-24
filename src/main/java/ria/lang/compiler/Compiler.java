package ria.lang.compiler;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import ria.lang.Core;
import ria.lang.Fun;
import ria.lang.Struct3;
import ria.lang.compiler.code.Code;
import ria.lang.compiler.code.JavaClass;
import ria.lang.compiler.code.RootClosure;
import ria.lang.compiler.code.SeqExpr;
import ria.lang.compiler.code.StructConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Compiler implements Opcodes {
    static final int CF_RESOLVE_MODULE = 1;
    static final int CF_PRINT_PARSE_TREE = 2;
    static final int CF_EVAL = 4;
    static final int CF_EVAL_RESOLVE = 8;
    static final int CF_EVAL_STORE = 32;
    static final int CF_EVAL_BIND = 40;
    static final int CF_EXPECT_MODULE = 128;
    static final int CF_EXPECT_PROGRAM = 256;
    // hack to force getting riadoc on doc generation
    static final int CF_FORCE_COMPILE = 512;
    // used with CF_RESOLVE_MODULE on preload
    static final int CF_IGNORE_CLASSPATH = 1024;

    // global flags
    static final int GF_NO_IMPORT = 16;
    static final int GF_DOC = 64;

    static final String[] PRELOAD = new String[]{"ria/lang/std", "ria/lang/io"};

    static final ThreadLocal<Compiler> currentCompiler = new ThreadLocal<>();
    private static ClassLoader JAVAC;
    public final List<JavaClass> postGen = new ArrayList<>();
    final Map<String, ModuleType> types = new HashMap<>();
    final Map<String, CType> opaqueTypes = new HashMap<>();
    // We should cache JavaTypes at some point, reflection is expensive
    //final Map X = new HashMap();
    Fun writer;
    String depDestDir; // used to read already compiled classes
    private static final String sourceCharset = "UTF-8";
    Fun customReader;
    ClassFinder classPath;
    String[] preload = PRELOAD;
    int classWriterFlags = ClassWriter.COMPUTE_FRAMES;
    int globalFlags;
    private Map<String, ModuleType> compiled = new HashMap<>();
    private List<CompileException> warnings = new ArrayList<>();
    private String currentSrc;
    private Map<String, Object> definedClasses = new HashMap<>();
    private String[] sourcePath = {};

    Compiler() {
    }

    void warn(CompileException ex) {
        ex.fn = currentSrc;
        warnings.add(ex);
    }

    public String createClassName(Context context, String outerClass, String nameBase) {
        boolean anon = Objects.equals(nameBase, "") && context != null;
        nameBase = outerClass + '$' + nameBase;
        String lower = nameBase.toLowerCase(), name = lower;
        int counter = -1;

        if(anon) {
            name = lower + (counter = context.constants.anonymousClassCounter);
        }
        while(definedClasses.containsKey(name)) {
            name = lower + ++counter;
        }
        if(anon) {
            context.constants.anonymousClassCounter = counter + 1;
        }
        return counter < 0 ? nameBase : nameBase + counter;
    }

    public void enumWarns(Fun f) {
        for(Object warning : warnings) {
            f.apply(warning);
        }
    }

    private void generateModuleAccessors(Map<String, CType> fields, Context context, Map<String, Code> direct) {
        for(Map.Entry<String, CType> entry : fields.entrySet()) {
            String name = entry.getKey();
            String jname = Code.mangle(name);
            String fname = name.equals("eval") ? "eval$" : jname;
            String type = Code.javaType(entry.getValue());
            String descr = "()L" + type + ';';

            Context m = context.newMethod(ACC_PUBLIC | ACC_STATIC, fname, descr);
            Code v = direct.get(name);
            if(v != null) { // constant
                v.gen(m);
                m.typeInsn(CHECKCAST, type);
            } else if(direct.containsKey(name)) { // mutable
                m.methodInsn(INVOKESTATIC, context.className, "eval",
                    "()Ljava/lang/Object;");
                m.ldcInsn(name);
                m.methodInsn(INVOKEINTERFACE, "ria/lang/Struct", "get",
                    "(Ljava/lang/String;)Ljava/lang/Object;");
                m.typeInsn(CHECKCAST, type);
            } else { // through static field
                descr = descr.substring(2);
                context.cw.visitField(ACC_PRIVATE | ACC_STATIC, jname,
                    descr, null, null).visitEnd();
                context.insn(DUP);
                context.ldcInsn(name);
                context.methodInsn(INVOKEINTERFACE, "ria/lang/Struct", "get",
                    "(Ljava/lang/String;)Ljava/lang/Object;");
                context.typeInsn(CHECKCAST, type);
                context.fieldInsn(PUTSTATIC, context.className, jname, descr);

                genFastInit(m);
                m.fieldInsn(GETSTATIC, context.className, jname, descr);
            }
            m.insn(ARETURN);
            m.closeMethod();
        }
    }

    String compileAll(String[] sources, int flags, String[] javaArg)
        throws Exception {
        List<String> java = null;
        int i, riaCount = 0;
        for(i = 0; i < sources.length; ++i) {
            if(sources[i].endsWith(".java")) {
                char[] s = readSourceFile(null, sources[i], new RiaAnalyzer());
                new JavaSource(sources[i], s, classPath.parsed);
                if(java == null) {
                    java = new ArrayList<>();
                    boolean debug = true;
                    for(String aJavaArg : javaArg) {
                        if(aJavaArg.startsWith("-g")) {
                            debug = false;
                        }
                        java.add(aJavaArg);
                    }
                    if(!java.contains("-encoding")) {
                        java.add("-encoding");
                        java.add("utf-8");
                    }
                    if(debug) {
                        java.add("-g");
                    }
                    if(classPath.pathStr.length() != 0) {
                        java.add("-classpath");
                        String path = classPath.pathStr;
                        if(depDestDir != null) {
                            path = path.length() == 0 ? depDestDir
                                : path + File.pathSeparator + depDestDir;
                        }
                        java.add(path);
                    }
                }
                java.add(sources[i]);
            } else {
                sources[riaCount++] = sources[i];
            }
        }
        String mainClass = null;
        for(i = 0; i < riaCount; ++i) {
            String className = compile(sources[i], null, flags).name;
            if(!types.containsKey(className)) {
                mainClass = className;
            }
        }
        if(java != null) {
            javaArg = java.toArray(new String[javaArg.length]);
            Class javac = null;
            try {
                javac = Class.forName("com.sun.tools.javac.Main", true, getClass().getClassLoader());
            } catch(Exception ignored) {
            }
            java.lang.reflect.Method m;
            try {
                if(javac == null) { // find javac...
                    synchronized(currentCompiler) {
                        if(JAVAC == null) {
                            File f = new File(System.getProperty("java.home"), "../lib/tools.jar");
                            if(!f.exists()) {
                                f = new File(System.getenv("JAVA_HOME"), "lib/tools.jar");
                            }
                            JAVAC = new URLClassLoader(new URL[]{f.toURI().toURL()});
                        }
                    }
                    javac = Class.forName("com.sun.tools.javac.Main", true, JAVAC);
                }
                //noinspection unchecked - Cannot avoid this because Java generics - sigh...
                m = javac.getMethod("compile", String[].class);
            } catch(Exception ex) {
                throw new CompileException(null, "Couldn't find Java compiler");
            }
            Object o = javac.newInstance();
            if((Integer)m.invoke(o, new Object[]{javaArg}) != 0) {
                throw new CompileException(null, "Error while compiling Java sources");
            }
        }
        return riaCount != 0 ? mainClass : "";
    }

    void setSourcePath(String[] path) throws IOException {
        String[] sp = new String[path.length];
        for(int i = 0, j, cnt; i < path.length; ++i) {
            String s = path[i];
            char c = ' '; // check URI
            for(j = 0, cnt = s.length(); j < cnt; ++j) {
                if(((c = s.charAt(j)) < 'a' || c > 'z') &&
                    (c < '0' || c > '9')) {
                    break;
                }
            }
            sp[i] = j > 1 && c == ':' ? s : new File(s).getCanonicalPath();
        }
        sourcePath = sp;
    }

    private char[] readSourceFile(String parent, String fn, RiaAnalyzer analyzer) throws IOException {
        if(customReader != null) {
            Struct3 arg = new Struct3(new String[]{"name", "parent"}, null);
            arg._0 = fn;
            arg._1 = parent == null ? Core.UNDEF_STR : parent;
            String result = (String)customReader.apply(arg);
            if(!Objects.equals(result, Core.UNDEF_STR)) {
                analyzer.canonicalFile = (String)arg._0;
                analyzer.sourceFile = null;
                if(compiled.containsKey(analyzer.canonicalFile)) {
                    return null;
                }
                return result.toCharArray();
            }
        }
        File f = new File(parent, fn);
        analyzer.sourceFile = f.getName();
        if(parent == null) { // !loadModule
            f = f.getCanonicalFile();
            analyzer.canonicalFile = f.getPath();
            if(compiled.containsKey(analyzer.canonicalFile)) {
                return null;
            }
        }
        char[] buf = new char[0x8000];
        InputStream stream = new FileInputStream(f);
        Reader reader = null;
        try {
            reader = new java.io.InputStreamReader(stream, sourceCharset);
            int n, l = 0;
            while((n = reader.read(buf, l, buf.length - l)) >= 0) {
                if(buf.length - (l += n) < 0x1000) {
                    char[] tmp = new char[buf.length << 1];
                    System.arraycopy(buf, 0, tmp, 0, l);
                    buf = tmp;
                }
            }
        } finally {
            if(reader != null) {
                reader.close();
            } else {
                stream.close();
            }
        }
        if(parent != null) {
            analyzer.canonicalFile = f.getCanonicalPath();
        }
        analyzer.sourceTime = f.lastModified();
        return buf;
    }

    private void verifyModuleCase(RiaAnalyzer analyzer) {
        int l = analyzer.canonicalFile.length() - analyzer.sourceName.length();
        if(l < 0) {
            return;
        }
        String cf = analyzer.canonicalFile.substring(l);
        if(!analyzer.sourceName.equals(cf) &&
            analyzer.sourceName.equalsIgnoreCase(cf)) {
            throw new CompileException(0, 0, "Module file name case doesn't match the requested name");
        }
    }

    // if loadModule is true, the file is searched from the source path
    private char[] readSource(RiaAnalyzer analyzer) {
        try {
            if((analyzer.flags & CF_RESOLVE_MODULE) == 0) {
                return readSourceFile(null, analyzer.sourceName, analyzer);
            }
            // Search from path. The localName is slashed package name.
            final String name = analyzer.sourceName;
            String fn = analyzer.sourceName = name + ".ria";
            if(sourcePath.length == 0) {
                throw new IOException("no source path");
            }
            int sep = fn.lastIndexOf('/');
            for(; ; ) {
                // search _with_ packageName
                for(String aSourcePath : sourcePath) {
                    try {
                        char[] r = readSourceFile(aSourcePath, fn, analyzer);
                        analyzer.sourceDir = aSourcePath;
                        verifyModuleCase(analyzer);
                        return r;
                    } catch(IOException ignored) {
                    }
                }
                if(sep != -2 && (analyzer.flags & CF_IGNORE_CLASSPATH) == 0
                    && (analyzer.resolvedType = moduleType(name)) != null) {
                    return null;
                }
                if(sep <= 0) // no package path, fail
                {
                    throw new CompileException(0, 0, "Module " + name.replace('/', '.') + " not found");
                }
                fn = fn.substring(sep + 1); // try without package path
                sep = -2; // fail next time, without rechecking classpath
            }
        } catch(IOException e) {
            throw new CompileException(0, 0, analyzer.sourceName + ": " + e.getMessage());
        }
    }

    ModuleType moduleType(String name) throws IOException {
        String cname = name.toLowerCase();
        long[] lastModified = {-1};
        InputStream in = classPath.findClass(cname + ".class", lastModified);
        if(in == null) {
            return null;
        }
        ModuleType t = RiaTypeVisitor.readType(this, in);
        if(t != null) {
            t.name = cname;
            t.lastModified = lastModified[0];
            types.put(cname, t);
        }
        return t;
    }

    void deriveName(RiaParser parser, RiaAnalyzer analyzer) {
        if((analyzer.flags & (CF_EVAL | CF_RESOLVE_MODULE)) == CF_EVAL) {
            if(parser.moduleName == null) {
                parser.moduleName = "code";
            }
            if(sourcePath.length == 0) {
                sourcePath = new String[]{new File("").getAbsolutePath()};
            }
            return;
        }
        // derive or verify the module name
        String cf = analyzer.canonicalFile, name = null;
        int i, lastlen = -1, l;
        i = cf.length() - 4;
        if(i > 0 && cf.endsWith(".ria")) {
            cf = cf.substring(0, i);
        } else if(parser.isModule) {
            throw new CompileException(0, 0, "Ria module source file must have a .ria suffix");
        }
        boolean ok = parser.moduleName == null;
        String shortName = parser.moduleName;
        if(shortName != null) {
            l = shortName.lastIndexOf('/');
            shortName = l > 0 ? shortName.substring(l + 1) : null;
        }
        String[] path = analyzer.sourceDir == null ? sourcePath :
            new String[]{analyzer.sourceDir};
        for(i = 0; i < path.length; ++i) {
            l = path[i].length();
            if(l <= lastlen || cf.length() <= l ||
                cf.charAt(l) != File.separatorChar ||
                !path[i].equals(cf.substring(0, l))) {
                continue;
            }
            name = cf.substring(l + 1).replace(File.separatorChar, '/');
            if(!ok && (name.equalsIgnoreCase(parser.moduleName) ||
                name.equalsIgnoreCase(shortName))) {
                ok = true;
                break;
            }
            lastlen = l;
        }
        if(name == null) {
            name = new File(cf).getName();
        }

        if(!ok && (lastlen != -1 || !name.equalsIgnoreCase(shortName) &&
            !name.equalsIgnoreCase(parser.moduleName))) {
            throw new CompileException(parser.moduleNameLine, 0,
                (parser.isModule ? "module " : "program ") +
                    parser.moduleName.replace('/', '.') +
                    " is not allowed to be in file named '" +
                    analyzer.canonicalFile + "'");
        }
        if(parser.moduleName != null) {
            name = parser.moduleName;
        }
        parser.moduleName = parser.isModule ? name.toLowerCase() : name;

        if(sourcePath.length == 0) {
            l = cf.length() - (name.length() + 1);
            if(l >= 0) {
                name = cf.substring(l).replace(File.separatorChar, '/');
                if(l == 0) {
                    l = 1;
                }
                if(name.charAt(0) != '/' ||
                    !name.substring(1).equalsIgnoreCase(parser.moduleName)) {
                    l = -1;
                }
            }
            name = l < 0 ? new File(cf).getParent() : cf.substring(0, l);
            if(name == null) {
                name = new File("").getAbsolutePath();
            }
            sourcePath = new String[]{name};
        }

        name = parser.moduleName.toLowerCase();
        if(definedClasses.containsKey(name)) {
            throw new CompileException(0, 0, (definedClasses.get(name) == null
                ? "Circular module dependency: "
                : "Duplicate module name: ") + name.replace('/', '.'));
        }
        if(depDestDir != null && (analyzer.flags & CF_FORCE_COMPILE) == 0) {
            analyzer.targetFile = new File(depDestDir, parser.moduleName.concat(".class"));
            analyzer.targetTime = analyzer.targetFile.lastModified();
        }
    }

    ModuleType compile(String sourceName, char[] code, int flags)
        throws Exception {
        RiaAnalyzer analyzer = new RiaAnalyzer();
        analyzer.flags = flags;
        analyzer.compiler = this;
        analyzer.sourceName = sourceName;
        if(code == null) {
            code = readSource(analyzer);
            if(code == null) {
                return analyzer.resolvedType != null ? analyzer.resolvedType :
                    compiled.get(analyzer.canonicalFile);
            }
        }
        RootClosure codeTree;
        Compiler oldCompiler = currentCompiler.get();
        currentCompiler.set(this);
        String oldCurrentSrc = currentSrc;
        currentSrc = analyzer.sourceName;
        try {
            try {
                analyzer.preload = preload;
                codeTree = analyzer.toCode(code);
                if(codeTree == null) {
                    ModuleType t = analyzer.resolvedType;
                    if(t == null) { // module, type from class
                        t = RiaTypeVisitor.readType(this, new FileInputStream(analyzer.targetFile));
                        types.put(t.name, t);
                        t.topDoc = analyzer.topDoc;
                    }
                    t.lastModified = analyzer.targetTime;
                    t.hasSource = true;
                    compiled.put(analyzer.canonicalFile, t);
                    return t;
                }
            } finally {
                currentCompiler.set(oldCompiler);
            }
            final String name = codeTree.moduleType.name;
            if(name == null) {
                throw new CompileException(0, 0, "internal error: module/program name undefined");
            }
            ModuleType exists = types.get(name);

            if(exists != null && (flags & CF_FORCE_COMPILE) == 0) {
                return exists;
            }
            if(codeTree.isModule) {
                types.put(name, codeTree.moduleType);
            }
            if(writer != null) {
                generateCode(analyzer, codeTree);
            }
            compiled.put(analyzer.canonicalFile, codeTree.moduleType);
            classPath.existsCache.clear();
            currentSrc = oldCurrentSrc;
            return codeTree.moduleType;
        } catch(CompileException ex) {
            if(ex.fn == null) {
                ex.fn = analyzer.sourceName;
            }
            throw ex;
        }
    }

    private void generateCode(RiaAnalyzer anal, RootClosure codeTree) {
        String name = codeTree.moduleType.name;
        Constants constants = new Constants(anal.sourceName, anal.sourceFile);
        Context context = new Context(this, constants, null, null).newClass(ACC_PUBLIC |
            ACC_SUPER | (codeTree.isModule && codeTree.moduleType.deprecated
            ? ACC_DEPRECATED : 0), name, (anal.flags & CF_EVAL) != 0
            ? "ria/lang/Fun" : null, null, codeTree.line);
        constants.context = context;
        if(codeTree.isModule) {
            moduleEval(codeTree, context, name);
        } else if((anal.flags & CF_EVAL) != 0) {
            context.createInit(ACC_PUBLIC, "ria/lang/Fun");
            context = context.newMethod(ACC_PUBLIC, "apply",
                "(Ljava/lang/Object;)Ljava/lang/Object;");
            codeTree.gen(context);
            context.insn(ARETURN);
            context.closeMethod();
        } else {
            context = context.newMethod(ACC_PUBLIC | ACC_STATIC, "main",
                "([Ljava/lang/String;)V");
            context.localVarCount++;
            context.load(0).methodInsn(INVOKESTATIC, "ria/lang/Core",
                "setArgv", "([Ljava/lang/String;)V");
            Label codeStart = new Label();
            context.visitLabel(codeStart);
            codeTree.gen(context);
            context.insn(POP);
            context.insn(RETURN);
            Label exitStart = new Label();
            context.tryCatchBlock(codeStart, exitStart, exitStart,
                "ria/lang/ExitError");
            context.visitLabel(exitStart);
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/ExitError",
                "getExitCode", "()I");
            context.methodInsn(INVOKESTATIC, "java/lang/System", "exit", "(I)V");
            context.insn(RETURN);
            context.closeMethod();
        }
        constants.close();
        write(constants.unstoredClasses);
    }

    private void moduleEval(RootClosure codeTree, Context mctx, String name) {
        mctx.cw.visitField(ACC_PRIVATE | ACC_STATIC, "$",
            "Ljava/lang/Object;", null, null).visitEnd();
        mctx.cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_VOLATILE,
            "_$", "I", null, null);
        Context context = mctx.newMethod(ACC_PUBLIC | ACC_STATIC | ACC_SYNCHRONIZED,
            "eval", "()Ljava/lang/Object;");
        context.fieldInsn(GETSTATIC, name, "_$", "I");
        Label eval = new Label();
        context.jumpInsn(IFLE, eval);
        context.fieldInsn(GETSTATIC, name, "$", "Ljava/lang/Object;");
        context.insn(ARETURN);
        context.visitLabel(eval);
        context.intConst(-1); // mark in eval
        context.fieldInsn(PUTSTATIC, name, "_$", "I");
        Code codeTail = codeTree.body;
        while(codeTail instanceof SeqExpr) {
            codeTail = ((SeqExpr)codeTail).result;
        }
        Map<String, Code> direct;
        if(codeTail instanceof StructConstructor) {
            ((StructConstructor)codeTail).publish();
            codeTree.gen(context);
            direct = ((StructConstructor)codeTail).getDirect();
        } else {
            codeTree.gen(context);
            direct = new HashMap<>();
        }
        context.cw.visitAttribute(new TypeAttribute(codeTree.moduleType, this));
        if(codeTree.type.type == RiaType.STRUCT) {
            generateModuleAccessors(codeTree.type.allowedMembers, context, direct);
        }
        context.insn(DUP);
        context.fieldInsn(PUTSTATIC, name, "$", "Ljava/lang/Object;");
        context.intConst(1);
        context.fieldInsn(PUTSTATIC, name, "_$", "I");
        context.insn(ARETURN);
        context.closeMethod();
        context = mctx.newMethod(ACC_PUBLIC | ACC_STATIC, "init", "()V");
        genFastInit(context);
        context.insn(RETURN);
        context.closeMethod();
    }

    private void genFastInit(Context context) {
        context.fieldInsn(GETSTATIC, context.className, "_$", "I");
        Label ret = new Label();
        context.jumpInsn(IFNE, ret);
        context.methodInsn(INVOKESTATIC, context.className,
            "eval", "()Ljava/lang/Object;");
        context.insn(POP);
        context.visitLabel(ret);
    }

    void addClass(String name, Context context, int line) {
        if(definedClasses.put(name.toLowerCase(), context) != null) {
            throw new CompileException(line, 0, "Duplicate class: " + name.replace('/', '.'));
        }
        if(context != null) {
            context.constants.unstoredClasses.add(context);
        }
    }

    private void write(List<Context> unstoredClasses) {
        if(writer == null) {
            return;
        }
        int i, cnt = postGen.size();
        for(i = 0; i < cnt; ++i) {
            (postGen.get(i)).run();
        }
        postGen.clear();
        cnt = unstoredClasses.size();
        for(i = 0; i < cnt; ++i) {
            Context c = unstoredClasses.get(i);
            definedClasses.put(c.className.toLowerCase(), "");
            String name = c.className + ".class";
            byte[] content = c.cw.toByteArray();
            writer.apply(name, content);
            classPath.define(name, content);
        }
    }
}

