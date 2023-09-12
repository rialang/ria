package ria.lang.compiler;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import ria.lang.compiler.nodes.Node;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class RiaTypeVisitor extends ClassVisitor {
    TypeAttribute typeAttribute;
    private boolean deprecated;

    RiaTypeVisitor() {
        super(Opcodes.ASM9);
    }

    static ModuleType readType(Compiler compiler, InputStream in)
        throws IOException {
        RiaTypeVisitor visitor = new RiaTypeVisitor();
        ClassReader reader = new ClassReader(in);
        reader.accept(visitor, new Attribute[] {new TypeAttribute(null, compiler)},
            ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
        in.close();
        if(visitor.typeAttribute == null) {
            return null;
        }
        ModuleType mt = visitor.typeAttribute.moduleType;
        if(mt != null) {
            mt.deprecated = visitor.deprecated;
        }
        mt.name = reader.getClassName();
        mt.fromClass = true;
        return mt;
    }

    static ModuleType getType(Compiler ctx, Node node,
                              String name, boolean byPath) {
        final String cname = name.toLowerCase();
        ModuleType t = ctx.types.get(cname);
        if(t != null) {
            return t;
        }
        try {
            int flags = byPath ? Compiler.CF_FORCE_COMPILE : Compiler.CF_RESOLVE_MODULE;
            if(node == null && !byPath) {
                t = ctx.moduleType(cname);
                if(t != null) {
                    return t;
                }
                flags |= Compiler.CF_IGNORE_CLASSPATH;
            }
            t = ctx.types.get(ctx.compile(name, null,
                flags | Compiler.CF_EXPECT_MODULE).name);
            if(t == null) {
                throw new CompileException(node, "Could not compile `" + name + "' to a module");
            }
            if(!byPath && !cname.equals(t.name)) {
                throw new CompileException(node, "Found " +
                    t.name.replace('/', '.') +
                    " instead of " + name.replace('/', '.'));
            }
            return t;
        } catch(CompileException ex) {
            if(ex.line == 0) {
                if(node != null) {
                    ex.line = node.line;
                    ex.col = node.col;
                }
            }
            throw ex;
        } catch(RuntimeException ex) {
            throw ex;
        } catch(Exception ex) {
            throw new CompileException(node, ex.getMessage());
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        deprecated = (access & Opcodes.ACC_DEPRECATED) != 0;
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
        if(Objects.equals(attr.type, "RiaModuleType")) {
            if(typeAttribute != null) {
                throw new RuntimeException("Multiple RiaModuleType attributes are forbidden");
            }
            typeAttribute = (TypeAttribute)attr;
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value) {
        return null;
    }

    @Override
    public void visitInnerClass(String name, String outerName,
                                String innerName, int access) {
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        return null;
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
    }

    @Override
    public void visitSource(String source, String debug) {
    }
}
