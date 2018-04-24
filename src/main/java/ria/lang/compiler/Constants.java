package ria.lang.compiler;

import org.objectweb.asm.Opcodes;
import ria.lang.compiler.code.Code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Constants implements Opcodes {
    public final Map<Object, String> constants = new HashMap<>();
    public Map<String, String> structClasses = new HashMap<>();
    int anonymousClassCounter;
    String sourceName;
    public Context context;
    List<Context> unstoredClasses = new ArrayList<>();
    private Context sb;

    Constants(String sourceName, String sourceFile) {
        if(sourceFile == null) {
            if(sourceName != null) {
                int p = sourceName.lastIndexOf('/');
                if(p < 0) {
                    p = sourceName.lastIndexOf('\\');
                }
                sourceFile = p < 0 ? sourceName : sourceName.substring(p + 1);
            } else {
                sourceFile = "<>";
            }
        }
        this.sourceName = sourceFile;
    }

    private void constField(int mode, String name, Code code, String descr) {
        context.cw.visitField(mode, name, descr, null, null).visitEnd();
        if(sb == null) {
            sb = context.newMethod(ACC_STATIC, "<clinit>", "()V");
        }
        code.gen(sb);
        sb.fieldInsn(PUTSTATIC, context.className, name, descr);
    }

    void registerConstant(Object key, Code code, Context context_) {
        String descr = 'L' + Code.javaType(code.type.deref()) + ';';
        String name = constants.get(key);
        if(name == null) {
            name = "_".concat(Integer.toString(context.fieldCounter++));
            constField(ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, name, code, descr);
            constants.put(key, name);
        }
        context_.fieldInsn(GETSTATIC, context.className, name, descr);
    }

    void close() {
        if(sb != null) {
            sb.insn(RETURN);
            sb.closeMethod();
        }
    }

    // first value in array must be empty
    public void stringArray(Context context_, String[] array) {
        if(sb == null) {
            sb = context.newMethod(ACC_STATIC, "<clinit>", "()V");
        }
        array[0] = "Strings";
        List<String> key = Arrays.asList(array);
        String name = constants.get(key);
        if(name == null) {
            name = "_".concat(Integer.toString(context.fieldCounter++));
            context.cw.visitField(ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, name,
                "[Ljava/lang/String;", null, null).visitEnd();
            sb.intConst(array.length - 1);
            sb.typeInsn(ANEWARRAY, "java/lang/String");
            for(int i = 1; i < array.length; ++i) {
                sb.insn(DUP);
                sb.intConst(i - 1);
                sb.ldcInsn(array[i]);
                sb.insn(AASTORE);
            }
            sb.fieldInsn(PUTSTATIC, context.className, name, "[Ljava/lang/String;");
            constants.put(key, name);
        }
        context_.fieldInsn(GETSTATIC, context.className, name, "[Ljava/lang/String;");
    }

    // generates [Ljava/lang/String;[Z into stack, using constant cache
    public void structInitArg(Context context_, StructField[] fields,
                              int fieldCount, boolean nomutable) {
        String[] fieldNameArr = new String[fieldCount + 1];
        char[] mutableArr = new char[fieldNameArr.length];
        mutableArr[0] = '@';
        int i, mutableCount = 0;
        for(i = 1; i < fieldNameArr.length; ++i) {
            StructField f = fields[i - 1];
            fieldNameArr[i] = f.name;
            if(f.mutable || f.property > 0) {
                mutableArr[i] = '\001';
                ++mutableCount;
            }
        }
        stringArray(context_, fieldNameArr);
        if(nomutable || mutableCount == 0) {
            context_.insn(ACONST_NULL);
            return;
        }
        String key = new String(mutableArr);
        String name = constants.get(key);
        if(name == null) {
            name = "_".concat(Integer.toString(context.fieldCounter++));
            context.cw.visitField(ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC,
                name, "[Z", null, null).visitEnd();
            sb.intConst(fieldCount);
            sb.visitIntInsn(NEWARRAY, T_BOOLEAN);
            for(i = 0; i < fieldCount; ++i) {
                sb.insn(DUP);
                sb.intConst(i);
                sb.intConst(mutableArr[i + 1]);
                sb.insn(BASTORE);
            }
            sb.fieldInsn(PUTSTATIC, context.className, name, "[Z");
            constants.put(key, name);
        }
        context_.fieldInsn(GETSTATIC, context.className, name, "[Z");
    }
}
