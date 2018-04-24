package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.CType;
import ria.lang.compiler.CaptureWrapper;
import ria.lang.compiler.Context;
import ria.lang.compiler.JavaType;
import ria.lang.compiler.RiaType;

import java.util.Objects;

public class JavaExpr extends Code {
    Code object;
    JavaType.Method method;
    Code[] args;
    int line;

    JavaExpr(Code object, JavaType.Method method, Code[] args, int line) {
        this.object = object;
        this.method = method;
        this.args = args;
        this.line = line;
    }

    // Convert to java
    private static void convert(Context context, CType given, CType argType) {
        given = given.deref();
        argType = argType.deref();
        String descr = argType.javaType == null ? "" : argType.javaType.description;
        if(argType.type == RiaType.JAVA_ARRAY && given.type == RiaType.JAVA_ARRAY) {
            context.typeInsn(CHECKCAST, JavaType.descriptionOf(argType));
            return;
            // FIXME: when different array types are given
            // It should be picked up by the type system, but if cast is used, (potential) boom!
        }
        if(given.type != RiaType.JAVA &&
            (argType.type == RiaType.JAVA_ARRAY ||
                argType.type == RiaType.JAVA && argType.javaType.isCollection())) {
            // TODO: Check 't' for null - it should never happen, but well...
            CType t = argType.param.length != 0 ? argType.param[0].deref() : null;
            if(argType.type == RiaType.JAVA_ARRAY && t.javaType != null) {
                if(Objects.equals(t.javaType.description, "B")) {
                    context.typeInsn(CHECKCAST, "ria/lang/AbstractList");
                    context.methodInsn(INVOKESTATIC, "ria/lang/Core", "bytes", "(Lria/lang/AbstractList;)[B");
                    return;
                }
                if(t.javaType.description.charAt(0) == 'L') {
                    // TODO: This could be better optimised so we don't create the array here
                    // TODO: Then we would save the call to length and hte new array creation
                    context.typeInsn(CHECKCAST, "ria/lang/AbstractList");
                    context.methodInsn(INVOKESTATIC, "ria/lang/MutableList", "ofList", "(Lria/lang/AbstractList;)Lria/lang/MutableList;");
                    context.insn(DUP);
                    context.methodInsn(INVOKEVIRTUAL, "ria/lang/MutableList", "length", "()J");
                    context.insn(L2I);
                    new NewArrayExpr(argType, null, 0).gen(context);
                    context.methodInsn(INVOKEVIRTUAL, "ria/lang/MutableList", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");
                    descr = JavaType.descriptionOf(argType);
                    context.typeInsn(CHECKCAST, descr);
                    context.forceType(descr);
                    return;
                }
            }
            Label retry = new Label(), end = new Label();
            context.typeInsn(CHECKCAST, "ria/lang/AbstractIterator"); // i
            String tmpClass = !Objects.equals(descr, "Ljava/lang/Set;") ? "java/util/ArrayList" : "java/util/HashSet";
            context.typeInsn(NEW, tmpClass); // ia
            context.insn(DUP);               // iaa
            context.visitInit(tmpClass, "()V"); // ia
            context.insn(SWAP); // ai
            context.insn(DUP); // aii
            context.jumpInsn(IFNULL, end); // ai
            context.insn(DUP); // aii
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractIterator", "isEmpty", "()Z"); // aiz
            context.jumpInsn(IFNE, end); // ai
            context.visitLabel(retry);
            context.insn(DUP2); // aiai
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractIterator", "first", "()Ljava/lang/Object;");

            // Recursively convert types
            if(t != null && (t.type != RiaType.JAVA || t.javaType.description.length() > 1)) {
                convert(context, given.param[0], argType.param[0]);
            }
            // aiav
            context.methodInsn(INVOKEVIRTUAL, tmpClass, "add", "(Ljava/lang/Object;)Z"); // aiz
            context.insn(POP); // ai
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/AbstractIterator", "next", "()Lria/lang/AbstractIterator;"); // ai
            context.insn(DUP); // aii
            context.jumpInsn(IFNONNULL, retry); // ai
            context.visitLabel(end);
            context.insn(POP); // a
            if(argType.type != RiaType.JAVA_ARRAY) {
                return; // a - List/Set
            }

            String s = "";
            CType argArrayType = argType;
            while((argType = argType.param[0]).type == RiaType.JAVA_ARRAY) {
                //noinspection StringConcatenationInLoop
                s += "[";
                argArrayType = argType;
            }
            String arrayPrefix = s;
            if(Objects.equals(s, "") && argType.javaType.description.length() != 1) {
                s = argType.javaType.className();
            } else {
                s += argType.javaType.description;
            }
            context.insn(DUP); // aa
            context.methodInsn(INVOKEVIRTUAL, tmpClass,
                "size", "()I"); // an

            if(t.type != RiaType.JAVA ||
                (descr = t.javaType.description).length() != 1) {
                context.typeInsn(ANEWARRAY, s); // aA
                context.methodInsn(INVOKEVIRTUAL, tmpClass, "toArray",
                    "([Ljava/lang/Object;)[Ljava/lang/Object;");
                if(!s.equals("java/lang/Object")) {
                    context.typeInsn(CHECKCAST,
                        arrayPrefix + "[" + argType.javaType.description);
                }
                return; // A - object array
            }

            // emulate a for loop to fill primitive array
            int index = context.localVarCount++;
            Label next = new Label(), done = new Label();
            context.insn(DUP); // ann
            context.varInsn(ISTORE, index); // an
            new NewArrayExpr(argArrayType, null, 0).gen(context);
            context.insn(SWAP); // Aa
            context.visitLabel(next);
            context.varInsn(ILOAD, index); // Aan
            context.jumpInsn(IFEQ, done); // Aa
            context.visitIntInsn(IINC, index); // Aa --index
            context.insn(DUP2); // AaAa
            context.varInsn(ILOAD, index); // AaAan
            context.methodInsn(INVOKEVIRTUAL, tmpClass,
                "get", "(I)Ljava/lang/Object;"); // AaAv
            if(Objects.equals(descr, "Z")) {
                context.typeInsn(CHECKCAST, "java/lang/Boolean");
                context.methodInsn(INVOKEVIRTUAL, "java/lang/Boolean",
                    "booleanValue", "()Z");
            } else {
                context.typeInsn(CHECKCAST, "ria/lang/RiaNum");
                convertNum(context, descr);
            }
            context.varInsn(ILOAD, index); // AaAvn
            int insn = BASTORE;
            switch(argType.javaType.description.charAt(0)) {
                case 'D':
                    insn = DASTORE;
                    break;
                case 'F':
                    insn = FASTORE;
                    break;
                case 'I':
                    insn = IASTORE;
                    break;
                case 'J':
                    insn = LASTORE;
                    break;
                case 'S':
                    insn = SASTORE;
            }
            if(insn == DASTORE || insn == LASTORE) {
                // AaAvvn actually - long and double is 2 entries
                context.insn(DUP_X2); // AaAnvvn
                context.insn(POP);    // AaAnvv
            } else {
                context.insn(SWAP); // AaAnv
            }
            context.insn(insn); // Aa
            context.jumpInsn(GOTO, next); // Aa
            context.visitLabel(done);
            context.insn(POP); // A
            return; // A - primitive array
        }

        if(given.type == RiaType.STR) {
            context.typeInsn(CHECKCAST, "java/lang/String");
            // TODO: Check if we need this or not

            //context.insn(DUP);

            //context.fieldInsn(GETSTATIC, "ria/lang/Core", "UNDEF_STR",
            //    "Ljava/lang/String;");
            //Label defined = new Label();
            //context.jumpInsn(IF_ACMPNE, defined);
            //context.insn(POP);
            //context.insn(ACONST_NULL);
            //context.visitLabel(defined);
            return;
        }

        if(given.type != RiaType.NUM ||
            Objects.equals(descr, "Ljava/lang/Object;") ||
            Objects.equals(descr, "Ljava/lang/Number;")) {
            if(!Objects.equals(descr, "Ljava/lang/Object;")) {
                context.typeInsn(CHECKCAST, argType.javaType.className());
            }
            return;
        }
        // Convert numbers...
        context.typeInsn(CHECKCAST, "ria/lang/RiaNum");
        if(Objects.equals(descr, "Ljava/math/BigInteger;")) {
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/RiaNum",
                "toBigInteger", "()Ljava/math/BigInteger;");
            return;
        }
        if(Objects.equals(descr, "Ljava/math/BigDecimal;")) {
            context.methodInsn(INVOKEVIRTUAL, "ria/lang/RiaNum",
                "toBigDecimal", "()Ljava/math/BigDecimal;");
            return;
        }
        String newInstr = null;
        if(descr.startsWith("Ljava/lang/")) {
            newInstr = argType.javaType.className();
            context.typeInsn(NEW, newInstr);
            context.insn(DUP_X1);
            context.insn(SWAP);
            descr = Objects.equals(descr, "Ljava/lang/Long;") ? "J" : descr.substring(11, 12);
        }
        convertNum(context, descr);
        if(newInstr != null) {
            context.visitInit(newInstr, "(" + descr + ")V");
        }
    }

    // Convert a number to the Java equivalent
    private static void convertNum(Context context, String descr) {
        String method = null;
        switch(descr.charAt(0)) {
            case 'B':
                method = "byteValue";
                break;
            case 'D':
                method = "doubleValue";
                break;
            case 'F':
                method = "floatValue";
                break;
            case 'I':
                method = "intValue";
                break;
            case 'L':
                return;
            case 'J':
                method = "longValue";
                break;
            case 'S':
                method = "shortValue";
                break;
        }
        context.methodInsn(INVOKEVIRTUAL, "ria/lang/RiaNum",
            method, "()" + descr);
    }

    static void convertedArg(Context context, Code arg, CType argType, int line) {
        argType = argType.deref();
        if(argType.type == RiaType.JAVA) {
            // integer arguments can be directly generated
            String desc = argType.javaType.description;
            if(Objects.equals(desc, "I") || Objects.equals(desc, "J")) {
                arg.genInt(context, line, Objects.equals(desc, "J"));
                return;
            }
        }
        if(genRawArg(context, arg, argType, line)) {
            convert(context, arg.type, argType);
        } else if(argType.type == RiaType.STR) {
            convertValue(context, arg.type.deref()); // for as cast
        }
    }

    private static boolean genRawArg(Context context, Code arg, CType argType, int line) {
        CType given = arg.type.deref();
        String descr = argType.javaType == null ? null : argType.javaType.description;
        if(Objects.equals(descr, "Z")) {
            // boolean
            Label end = new Label(), lie = new Label();
            arg.genIf(context, lie, false);
            context.intConst(1);
            context.jumpInsn(GOTO, end);
            context.visitLabel(lie);
            context.intConst(0);
            context.visitLabel(end);
            return false;
        }
        arg.gen(context);
        if(given.type == RiaType.UNIT) {
            if(!(arg instanceof UnitConstant)) {
                context.insn(POP);
                context.insn(ACONST_NULL);
            }
            return false;
        }
        context.visitLine(line);
        if(Objects.equals(descr, "C")) {
            context.typeInsn(CHECKCAST, "java/lang/String");
            context.intConst(0);
            context.methodInsn(INVOKEVIRTUAL,
                "java/lang/String", "charAt", "(I)C");
            return false;
        }
        if(argType.type == RiaType.JAVA_ARRAY && given.type == RiaType.STR) {
            context.typeInsn(CHECKCAST, "java/lang/String");
            context.methodInsn(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C");
            return false;
        }
        if(arg instanceof StringConstant || arg instanceof ConcatStrings) {
            return false;
        }
        // conversion from array to list
        if(argType.type == RiaType.MAP && given.type == RiaType.JAVA_ARRAY) {
            JavaType javaItem = given.param[0].javaType;
            if(javaItem != null && javaItem.description.length() == 1) {
                String arrayType = "[".concat(javaItem.description);
                context.typeInsn(CHECKCAST, arrayType);
                context.methodInsn(INVOKESTATIC, "ria/lang/RiaArray", "wrap", "(" + arrayType + ")Lria/lang/AbstractList;");
                return false;
            }
            Label isNull = new Label(), end = new Label();
            context.typeInsn(CHECKCAST, "[Ljava/lang/Object;");
            context.insn(DUP);
            context.jumpInsn(IFNULL, isNull);
            boolean toList = argType.param[1].deref().type == RiaType.NONE;
            if(toList) {
                context.insn(DUP);
                context.insn(ARRAYLENGTH);
                context.jumpInsn(IFEQ, isNull);
            }
            if(toList && argType.param[0].deref().type == RiaType.STR) {
                // convert null's to undef_str's
                context.methodInsn(INVOKESTATIC, "ria/lang/MutableList", "ofStrArray", "([Ljava/lang/Object;)Lria/lang/MutableList;");
            } else {
                context.typeInsn(NEW, "ria/lang/MutableList");
                context.insn(DUP_X1);
                context.insn(SWAP);
                context.visitInit("ria/lang/MutableList", "([Ljava/lang/Object;)V");
            }
            context.jumpInsn(GOTO, end);
            context.visitLabel(isNull);
            context.insn(POP);
            if(toList) {
                context.insn(ACONST_NULL);
            } else {
                context.typeInsn(NEW, "ria/lang/MutableList");
                context.insn(DUP);
                context.visitInit("ria/lang/MutableList", "()V");
            }
            context.visitLabel(end);
            return false;
        }
        return argType.type == RiaType.JAVA ||
            argType.type == RiaType.JAVA_ARRAY;
    }

    static void genValue(Context context, Code arg, CType argType, int line) {
        genRawArg(context, arg, argType, line);
        if(arg.type.deref().type == RiaType.NUM && argType.javaType.description.length() == 1) {
            context.typeInsn(CHECKCAST, "ria/lang/RiaNum");
            convertNum(context, argType.javaType.description);
        }
    }

    static void convertValue(Context context, CType t) {
        if(t.type != RiaType.JAVA) {
            return; // array, no automatic conversions
        }
        String descr = t.javaType.description;
        switch(descr) {
            case "V": // void
                context.insn(ACONST_NULL);
                break;
            case "Ljava/lang/String;": // String
                Label nonnull = new Label();
                // checkcast to not lie later the type with context.fieldInsn
                context.typeInsn(CHECKCAST, "java/lang/String");
                context.insn(DUP);
                context.jumpInsn(IFNONNULL, nonnull);
                context.insn(POP);
                context.fieldInsn(GETSTATIC, "ria/lang/Core", "UNDEF_STR",
                    "Ljava/lang/String;");
                context.visitLabel(nonnull);
                break;
            case "Z": // boolean
                Label skip = new Label(), end = new Label();
                context.jumpInsn(IFEQ, skip);
                context.fieldInsn(GETSTATIC, "java/lang/Boolean", "TRUE",
                    "Ljava/lang/Boolean;");
                context.jumpInsn(GOTO, end);
                context.visitLabel(skip);
                context.fieldInsn(GETSTATIC, "java/lang/Boolean", "FALSE",
                    "Ljava/lang/Boolean;");
                context.visitLabel(end);
                break;
            case "B": // byte
            case "S": // short
            case "I": // int
            case "J": // long
                if(Objects.equals(descr, "B")) {
                    context.intConst(0xff);
                    context.insn(IAND);
                }
                context.typeInsn(NEW, "ria/lang/IntNum");
                if(Objects.equals(descr, "J")) {
                    context.insn(DUP_X2);
                    context.insn(DUP_X2);
                    context.insn(POP);
                } else {
                    context.insn(DUP_X1);
                    context.insn(SWAP);
                }
                context.visitInit("ria/lang/IntNum", Objects.equals(descr, "J") ? "(J)V" : "(I)V");
                context.forceType("ria/lang/RiaNum");
                break;
            case "D": // double
            case "F": // float
                context.typeInsn(NEW, "ria/lang/FloatNum");
                if(Objects.equals(descr, "F")) {
                    context.insn(DUP_X1);
                    context.insn(SWAP);
                    context.insn(F2D);
                } else {
                    context.insn(DUP_X2);
                    context.insn(DUP_X2);
                    context.insn(POP);
                }
                context.visitInit("ria/lang/FloatNum", "(D)V");
                context.forceType("ria/lang/RiaNum");
                break;
            case "C": // char
                context.methodInsn(INVOKESTATIC, "java/lang/String",
                    "valueOf", "(C)Ljava/lang/String;");
                context.forceType("java/lang/String");
                break;
        }
    }

    void visitInvoke(Context context, int invokeInsn) {
        context.methodInsn(invokeInsn, method.classType.javaType.className(), method.name, method.descr(null));
    }

    void genCall(Context context, BindRef[] extraArgs, int invokeInsn) {
        for(int i = 0; i < args.length; ++i) {
            convertedArg(context, args[i], method.arguments[i], line);
        }
        if(extraArgs != null) {
            for(BindRef arg : extraArgs) {
                CaptureWrapper cw = arg.capture();
                if(cw == null) {
                    arg.gen(context);
                    context.captureCast(arg.captureType());
                } else {
                    cw.genPreGet(context);
                }
            }
        }
        context.visitLine(line);
        visitInvoke(context, invokeInsn);
        JavaType jt = method.returnType.javaType;
        if(jt != null && jt.description.charAt(0) == 'L') {
            context.forceType(jt.className());
        }
    }

    @Override
    public void gen(Context context) {
        throw new UnsupportedOperationException();
    }
}
