package ria.lang.compiler;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import ria.lang.compiler.code.Code;

import java.util.HashMap;
import java.util.Map;

public final class Context implements Opcodes {
    public Compiler compilation;
    public String className;
    public ClassWriter cw;
    public Constants constants;
    public Map<String, Object> usedMethodNames;
    public int localVarCount;
    public int fieldCounter;
    int lastLine;
    public int tainted; // if we are inside a loop, natural laws are suspended
    private MethodVisitor m;
    private int lastInsn = -1;
    private String lastType;

    Context(Compiler compilation, Constants constants,
            ClassWriter writer, String className) {
        this.compilation = compilation;
        this.constants = constants;
        this.cw = writer;
        this.className = className;
    }

    public Context newClass(int flags, String name, String extend,
                            String[] interfaces, int line) {
        Context context = new Context(compilation, constants, new RiaClassWriter(compilation.classWriterFlags), name);
        context.usedMethodNames = new HashMap<>();
        context.cw.visit(V1_8, flags, name, null,
            extend == null ? "java/lang/Object" : extend, interfaces);
        context.cw.visitSource(constants.sourceName, null);
        compilation.addClass(name, context, line);
        return context;
    }

    public String methodName(String name) {
        Map<String, Object> used = usedMethodNames;
        if(name == null) {
            name = "_" + used.size();
        } else if(used.containsKey(name) || name.startsWith("_")) {
            name += used.size();
        }
        used.put(name, null);
        return name;
    }

    public Context newMethod(int flags, String name, String type) {
        Context context = new Context(compilation, constants, cw, className);
        context.usedMethodNames = usedMethodNames;
        context.m = cw.visitMethod(flags, name, type, null, null);
        context.m.visitCode();
        return context;
    }

    public void markInnerClass(Context outer, int access) {
        String fn = className.substring(outer.className.length() + 1);
        outer.cw.visitInnerClass(className, outer.className, fn, access);
        cw.visitInnerClass(className, outer.className, fn, access);
    }

    public void closeMethod() {
        insn(-1);
        m.visitMaxs(0, 0);
        m.visitEnd();
    }

    public void createInit(int mod, String parent) {
        MethodVisitor m = cw.visitMethod(mod, "<init>", "()V", null, null);
        // super()
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKESPECIAL, parent, "<init>", "()V", false);
        m.visitInsn(RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
    }

    public void intConst(int n) {
        if(n >= -1 && n <= 5) {
            insn(n + 3);
        } else {
            insn(-1);
            if(n >= -32768 && n <= 32767) {
                m.visitIntInsn(n >= -128 && n <= 127 ? BIPUSH : SIPUSH, n);
            } else {
                m.visitLdcInsn(n);
            }
        }
    }

    public void visitLine(int line) {
        if(line != 0 && lastLine != line) {
            Label label = new Label();
            m.visitLabel(label);
            m.visitLineNumber(line, label);
            lastLine = line;
        }
    }

    public void genBoolean(Label label) {
        fieldInsn(GETSTATIC, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;");
        Label end = new Label();
        m.visitJumpInsn(GOTO, end);
        m.visitLabel(label);
        m.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;");
        m.visitLabel(end);
    }

    public void insn(int opcode) {
        if(lastInsn != -1 && lastInsn != -2) {
            if(lastInsn == ACONST_NULL && opcode == POP) {
                lastInsn = -1;
                return;
            }
            m.visitInsn(lastInsn);
        }
        lastInsn = opcode;
    }

    public void varInsn(int opcode, int var) {
        insn(-1);
        m.visitVarInsn(opcode, var);
    }

    public Context load(int var) {
        insn(-1);
        m.visitVarInsn(ALOAD, var);
        return this;
    }

    public void visitIntInsn(int opcode, int param) {
        insn(-1);
        if(opcode != IINC) {
            m.visitIntInsn(opcode, param);
        } else {
            m.visitIincInsn(param, -1);
        }
    }

    public void typeInsn(int opcode, String type) {
        if(opcode == CHECKCAST) {
            if(lastInsn == -2 && type.equals(lastType) ||
                lastInsn == ACONST_NULL) {
                return; // no cast necessary
            }
            insn(-2);
            lastType = type;
        } else {
            insn(-1);
        }
        m.visitTypeInsn(opcode, type);
    }

    public void captureCast(String type) {
        if(type.charAt(0) == 'L') {
            type = type.substring(1, type.length() - 1);
        }
        if(!type.equals("java/lang/Object")) {
            typeInsn(CHECKCAST, type);
        }
    }

    public void visitInit(String type, String descr) {
        insn(-2);
        m.visitMethodInsn(INVOKESPECIAL, type, "<init>", descr, false);
        lastType = type;
    }

    public void forceType(String type) {
        insn(-2);
        lastType = type;
    }

    public void fieldInsn(int opcode, String owner, String name, String desc) {
        if(owner == null || name == null || desc == null) {
            throw new IllegalArgumentException("fieldInsn(" + opcode + ", " + owner + ", " + name + ", " + desc + ")");
        }
        insn(-1);
        m.visitFieldInsn(opcode, owner, name, desc);
        if((opcode == GETSTATIC || opcode == GETFIELD) && desc.charAt(0) == 'L') {
            lastInsn = -2;
            lastType = desc.substring(1, desc.length() - 1);
        }
    }

    public void methodInsn(int opcode, String owner, String name, String desc) {
        insn(-1);
        m.visitMethodInsn(opcode, owner, name, desc, opcode == INVOKEINTERFACE);
    }

    public void visitApply(Code arg, int line) {
        arg.gen(this);
        insn(-1);
        visitLine(line);
        m.visitMethodInsn(INVOKEVIRTUAL, "ria/lang/Fun",
            "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
    }

    public void jumpInsn(int opcode, Label label) {
        insn(-1);
        m.visitJumpInsn(opcode, label);
    }

    public void visitLabel(Label label) {
        if(lastInsn != -2) {
            insn(-1);
        }
        m.visitLabel(label);
    }

    public void ldcInsn(Object cst) {
        insn(-1);
        m.visitLdcInsn(cst);
        if(cst instanceof String) {
            lastInsn = -2;
            lastType = "java/lang/String";
        }
    }

    public void tryCatchBlock(Label start, Label end, Label handler, String type) {
        insn(-1);
        m.visitTryCatchBlock(start, end, handler, type);
    }

    public void switchInsn(int min, int max, Label dflt,
                           int[] keys, Label[] labels) {
        insn(-1);
        if(keys == null) {
            m.visitTableSwitchInsn(min, max, dflt, labels);
        } else {
            m.visitLookupSwitchInsn(dflt, keys, labels);
        }
    }

    public void constant(Object key, Code code) {
        constants.registerConstant(key, code, this);
    }

    public void popn(int n) {
        if((n & 1) != 0) {
            insn(POP);
        }
        for(; n >= 2; n -= 2) {
            insn(POP2);
        }
    }
}
