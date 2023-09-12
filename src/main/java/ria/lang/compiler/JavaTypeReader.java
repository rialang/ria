package ria.lang.compiler;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JavaTypeReader extends ClassVisitor implements Opcodes {
    Map<String, CType> vars = new HashMap<>();
    Map<String, JavaType.Field> fields = new HashMap<>();
    Map<String, JavaType.Field> staticFields = new HashMap<>();
    public List<JavaType.Method> methods = new ArrayList<>();
    public List<JavaType.Method> staticMethods = new ArrayList<>();
    public List<JavaType.Method> constructors = new ArrayList<>();
    public JavaType parent;
    public String className;
    public String[] interfaces;
    public int access;

    public JavaTypeReader() {
        super(ASM9);
    }

    private static int parseSig(Map<String, CType> vars, List<CType> res, int p, char[] s) {
        int arrays = 0;
        for(int l = s.length; p < l && s[p] != '>'; ++p) {
            if(s[p] == '+' || s[p] == '*') {
                continue;
            }
            if(s[p] == '[') {
                ++arrays;
                continue;
            }
            if(s[p] == ')') {
                continue;
            }
            CType t;
            if(s[p] == 'L') {
                // Handle class name
                int p1 = p;
                while(p < l && s[p] != ';' && s[p] != '<') {
                    ++p;
                }
                t = new CType(new String(s, p1, p - p1).concat(";"));
                if(p < l && s[p] == '<') {
                    List<CType> param = new ArrayList<>();
                    p = parseSig(vars, param, p + 1, s) + 1;
                    // TODO: Get this working with Java generics
                    // TODO: broken generics support
                    // TODO: strip free type vars from classes...
                    /*for (int i = param.size(); --i >= 0;) {
                        if (param.get(i).type == RiaType.VAR) {
                            param.remove(i);
                        }
                    }*/
                    t.param = param.toArray(new CType[0]);
                    // new type for java generics
                    //t.type = RiaType.JAVA_GENERIC;
                }
            } else if(s[p] == 'T') {
                // Handle generic types
                int p1 = p + 1;
                while(++p < l && s[p] != ';' && s[p] != '<') {
                }
                /*String varName = new String(s, p1, p - p1);
                t = vars.get(varName);
                if (t == null) {
                    t = new CType(1000000);
                    vars.put(varName, t);
                }*/
                t = RiaType.OBJECT_TYPE;
            } else {
                // Primitives
                t = new CType(new String(s, p, 1));
            }
            for(; arrays > 0; --arrays) {
                // Handle arrays
                t = new CType(RiaType.JAVA_ARRAY, new CType[]{t});
            }
            res.add(t);
        }
        return p;
    }

    public static CType[] parseSig1(int start, String sig) {
        List<CType> res = new ArrayList<>();
        parseSig(new HashMap<>(), res, start, sig.toCharArray());
        return res.toArray(new CType[0]);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if(superName != null) {
            parent = JavaType.fromDescription('L' + superName + ';');
        }
        this.access = access;
        this.interfaces = interfaces;
    }

    @Override
    public void visitEnd() {
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return null;
    }

    @Override
    public void visitAttribute(Attribute attr) {
    }

    private List<CType> parseSig(int start, String sig) {
        List<CType> res = new ArrayList<>();
        parseSig(vars, res, start, sig.toCharArray());
        return res;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if((access & ACC_PRIVATE) == 0) {
            List<CType> l = parseSig(0, signature == null ? desc : signature);
            JavaType.Field f = new JavaType.Field(name, access, className, l.get(0));
            if((access & (ACC_FINAL | ACC_STATIC)) == (ACC_FINAL | ACC_STATIC)) {
                f.constValue = value;
            }
            (((access & ACC_STATIC) == 0) ? fields : staticFields).put(name, f);
        }
        return null;
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        if((access & ACC_PRIVATE) == 0) {
            JavaType.Method m = new JavaType.Method();
            // TODO: Workaround for broken generics support
            // TODO: Fixme
            signature = desc;
            List<CType> l = parseSig(1, signature);
            m.sig = name + signature;
            m.name = name;
            m.access = access;
            int argc = l.size() - 1;
            m.returnType = l.get(argc);

            m.arguments = l.subList(0, argc).toArray(new CType[argc]);
            m.className = className;
            if(Objects.equals(m.name, "<init>")) {
                constructors.add(m);
            } else if((access & ACC_STATIC) == 0) {
                methods.add(m);
            } else {
                staticMethods.add(m);
            }
        }
        return null;
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
    }

    @Override
    public void visitSource(String source, String debug) {
    }
}
