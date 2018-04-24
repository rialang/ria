package ria.lang.compiler.code;

import org.objectweb.asm.Label;
import ria.lang.compiler.Binder;
import ria.lang.compiler.CType;
import ria.lang.compiler.CaptureWrapper;
import ria.lang.compiler.Context;
import ria.lang.compiler.RiaType;
import ria.lang.compiler.StructField;
import ria.lang.compiler.code.Apply;
import ria.lang.compiler.code.BindRef;
import ria.lang.compiler.code.LoadVar;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/*
 * Being a closure allows inlining property getters/setters.
 */
public final class StructConstructor extends CapturingClosure implements Comparator<Object> {
    StructField[] fields;
    StructField[] fieldsOrigOrder;
    int fieldCount;
    public StructField properties;
    String impl;
    int arrayVar = -1;
    private boolean mustGen;
    private Code withParent;
    private String[] withFields;

    public StructConstructor(int maxBinds) {
        fields = new StructField[maxBinds];
    }

    // for some reason, binds are only for non-property fields
    public Binder bind(StructField sf) {
        return new Bind(sf);
    }

    public void add(StructField field) {
        if(field.name == null) {
            throw new IllegalArgumentException();
        }
        fields[fieldCount++] = field;
        if(field.property != 0) {
            field.nextProperty = properties;
            properties = field;
        }
    }

    @Override
    public int compare(Object a, Object b) {
        return ((StructField)a).name.compareTo(((StructField)b).name);
    }

    public void close() {
        // warning - close can be called second time by `with'
        fieldsOrigOrder = new StructField[fields.length];
        System.arraycopy(fields, 0, fieldsOrigOrder, 0, fields.length);
        Arrays.sort(fields, 0, fieldCount, this);
        for(int i = 0; i < fieldCount; ++i) {
            StructField field = fields[i];
            field.javaName = "_".concat(Integer.toString(i));
            field.index = i;
            if(field.property != 0) {
                mustGen = true;
            }
        }
    }

    public void publish() {
        for(int i = 0; i < fieldCount; ++i) {
            if(fields[i].property <= 0) {
                Code v = fields[i].value;
                while(v instanceof BindRef) {
                    v = ((BindRef)v).unref(true);
                }
                if(v instanceof Function) {
                    ((Function)v).publish = true;
                }
            }
        }
    }

    public Map<String, Code> getDirect() {
        Map<String, Code> r = new HashMap<>(fieldCount);
        for(int i = 0; i < fieldCount; ++i) {
            if(fields[i].mutable || fields[i].property > 0) {
                r.put(fields[i].name, null); // disable
                continue;
            }
            if(fields[i].binder != null) {
                continue;
            }
            Code v = fields[i].value;
            while(v instanceof BindRef) {
                v = ((BindRef)v).unref(false);
            }
            if(v != null && v.flagop(CONST)) {
                r.put(fields[i].name, v);
            }
        }
        return r;
    }

    @Override
    void captureInit(Context st, Capture c, int n) {
        // c.getId() initialises the captures id as a side effect
        st.cw.visitField(ACC_SYNTHETIC, c.getId(st), c.captureType(),
            null, null).visitEnd();
    }

    private void initBinders(Context context) {
        for(int i = 0; i < fieldCount; ++i) {
            if(fields[i].binder != null) {
                ((Bind)fields[i].binder).initGen(context);
            }
        }
    }

    @Override
    public void gen(Context context) {
        boolean generated = false;
        // mustgen is set to true if we use the with operator
        // in that case we need to provide a constructor that
        // will accept the parent structure. Therefore we need a
        // new class.
        if(mustGen || fieldCount > 3 && fieldCount <= 15) {
            impl = genStruct(context);
            generated = true;
        } else {
            if(fieldCount <= 3) {
                impl = "ria/lang/Struct3";
            }
            initBinders(context);
        }
        String implClass = impl != null ? impl : "ria/lang/GenericStruct";
        context.typeInsn(NEW, implClass);
        context.insn(DUP);
        if(withParent != null) {
            withParent.gen(context);
            context.visitInit(implClass, "(Lria/lang/Struct;)V");
        } else if(generated) {
            context.visitInit(implClass, "()V");
        } else {
            context.constants.structInitArg(context, fields, fieldCount, false);
            context.visitInit(implClass, "([Ljava/lang/String;[Z)V");
        }
        if(arrayVar != -1) {
            context.varInsn(ASTORE, arrayVar);
        }
        for(int i = 0, cnt = fieldCount; i < cnt; ++i) {
            if(fields[i].property != 0 || fields[i].inherited) {
                continue;
            }
            if(arrayVar != -1) {
                context.load(arrayVar);
            } else {
                context.insn(DUP);
            }
            if(impl == null) {
                context.ldcInsn(fields[i].name);
            }
            if(fields[i].binder != null) {
                fields[i].binder.gen(context);
                ((Function)fields[i].value).finishGen(context);
            } else {
                fields[i].value.gen(context);
            }
            if(impl != null) {
                context.fieldInsn(PUTFIELD, impl, fields[i].javaName,
                    "Ljava/lang/Object;");
            } else {
                context.methodInsn(INVOKEVIRTUAL, "ria/lang/GenericStruct",
                    "set", "(Ljava/lang/String;Ljava/lang/Object;)V");
            }
        }
        if(arrayVar != -1) {
            context.load(arrayVar);
        }
        for(Capture c = captures; c != null; c = c.next) {
            context.insn(DUP);
            c.captureGen(context);
            context.fieldInsn(PUTFIELD, impl, c.id, c.captureType());
        }
    }

    // Here we build a struct with the required number of fields
    private String genStruct(final Context context) {
        String cn, structKey = null, i_str;
        StructField field;
        Label next, dflt = null, jumps[];
        int i;

        if(mustGen) {
            /*
             * Have to generate our own struct class anyway, so take advantage
             * and change constant fields into inlined properties, eliminating
             * actual field stores for these fields in the structure.
             *
             * NOTE: This only should be done when the struct is first generated,
             * otherwise we will get errors when unifying the types as the field.property
             * will already be set to the wrong value.
             */
            for(i = 0; i < fieldCount; ++i) {
                field = fields[i];
                if(!field.mutable && field.property == 0 &&
                    !field.inherited && field.value.prepareConst(context)) {
                    field.property = -1;
                }
            }
        } else {
            // We look up the class we have already generated
            StringBuilder buf = new StringBuilder();
            for(i = 0; i < fields.length; ++i) {
                buf.append(fields[i].mutable ? ';' : ',')
                    .append(fields[i].name);
            }
            structKey = buf.toString();
            cn = context.constants.structClasses.get(structKey);
            if(cn != null) {
                initBinders(context);
                return cn;
            }
        }

        cn = context.compilation.createClassName(context, context.className, "");
        if(structKey != null) {
            context.constants.structClasses.put(structKey, cn);
        }
        Context st = context.newClass(ACC_SUPER | ACC_FINAL, cn, "ria/lang/AbstractStruct", null,
            fieldsOrigOrder[0].line);
        st.fieldCounter = fieldCount;
        mergeCaptures(st, true);
        Context m = st.newMethod(ACC_PUBLIC, "<init>",
            withParent == null ? "()V" : "(Lria/lang/Struct;)V");
        m.load(0).constants.structInitArg(m, fields, fieldCount, withParent != null);
        m.visitInit("ria/lang/AbstractStruct", "([Ljava/lang/String;[Z)V");
        if(withParent != null) {
            // generates code for joining super fields
            m.intConst(2);
            m.visitIntInsn(NEWARRAY, T_INT);
            m.varInsn(ASTORE, 4); // index - int[]
            m.intConst(withFields.length - 2);
            m.varInsn(ISTORE, 3); // j = NAMES.length - 1
            // ext (extended struct)
            m.load(1).methodInsn(INVOKEINTERFACE, "ria/lang/Struct", "count", "()I");
            m.varInsn(ISTORE, 2); // i - field counter
            Label retry = new Label(), cont = new Label(), exit = new Label();
            next = new Label();
            m.visitLabel(retry);
            m.visitIntInsn(IINC, 2); // --i

            // if (ext.name(i) != NAMES[j]) goto next;
            m.load(1).varInsn(ILOAD, 2);
            m.methodInsn(INVOKEINTERFACE, "ria/lang/Struct",
                "name", "(I)Ljava/lang/String;");
            m.constants.stringArray(m, withFields);
            m.varInsn(ILOAD, 3);
            m.insn(AALOAD); // NAMES[j]
            m.jumpInsn(IF_ACMPNE, cont);

            // this ext.ref(i, index, 0)
            m.load(0).load(1).varInsn(ILOAD, 2);
            m.load(4).intConst(0);
            m.methodInsn(INVOKEINTERFACE, "ria/lang/Struct", "ref", "(I[II)Ljava/lang/Object;");

            jumps = new Label[withFields.length - 1];
            for(i = 0; i < jumps.length; ++i) {
                jumps[i] = new Label();
            }
            m.load(0).load(4).intConst(0);
            m.insn(IALOAD); // index[0]
            m.load(0).load(4).intConst(1);
            m.insn(IALOAD); // index[1]
            if(jumps.length > 1) {
                dflt = new Label();
                m.varInsn(ILOAD, 3); // switch (j)
                m.switchInsn(0, jumps.length - 1, dflt, null, jumps);
            }
            i = 0;
            for(int j = 0; j < jumps.length; ++i) {
                if(fields[i].inherited) {
                    m.visitLabel(jumps[j++]);
                    i_str = Integer.toString(i);
                    m.fieldInsn(PUTFIELD, cn, "h".concat(i_str), "Z");
                    m.fieldInsn(PUTFIELD, cn, "i".concat(i_str), "I");
                    m.fieldInsn(PUTFIELD, cn, fields[i].javaName,
                        "Ljava/lang/Object;");
                    m.jumpInsn(GOTO, next);
                }
            }
            if(jumps.length > 1) {
                m.visitLabel(dflt);
                m.popn(6); // this ref this index this hidden
            }
            m.visitLabel(next);
            m.visitIntInsn(IINC, 3); // --j
            m.varInsn(ILOAD, 3);
            m.jumpInsn(IFLT, exit);

            m.visitLabel(cont);
            m.varInsn(ILOAD, 2);
            m.jumpInsn(IFGT, retry);
            m.visitLabel(exit);
        }
        m.insn(RETURN);
        m.closeMethod();

        // fields
        for(i = 0; i < fieldCount; ++i) {
            field = fields[i];
            if(field.property == 0) {
                st.cw.visitField(field.inherited ? ACC_PRIVATE | ACC_FINAL : ACC_SYNTHETIC,
                    field.javaName, "Ljava/lang/Object;", null, null).visitEnd();
            }
            if(field.inherited) {
                i_str = Integer.toString(i);
                st.cw.visitField(ACC_PRIVATE | ACC_FINAL, "i".concat(i_str),
                    "I", null, null).visitEnd();
                st.cw.visitField(ACC_PRIVATE | ACC_FINAL, "h".concat(i_str),
                    "Z", null, null).visitEnd();
            }
        }

        // get(String)
        m = st.newMethod(ACC_PUBLIC, "get", "(Ljava/lang/String;)Ljava/lang/Object;");
        m.load(0);
        Label withMutable = null;
        for(i = 0; i < fieldCount; ++i) {
            next = new Label();
            field = fieldsOrigOrder[i];
            m.load(1).ldcInsn(field.name);
            m.jumpInsn(IF_ACMPNE, next);
            if(field.property != 0) {
                m.intConst(field.index);
                m.methodInsn(INVOKEVIRTUAL, cn, "get", "(I)Ljava/lang/Object;");
            } else {
                m.fieldInsn(GETFIELD, cn, field.javaName, "Ljava/lang/Object;");
            }
            if(field.inherited) {
                if(withMutable == null) {
                    withMutable = new Label();
                }
                m.load(0).fieldInsn(GETFIELD, cn, "i" + field.index, "I");
                m.insn(DUP);
                m.jumpInsn(IFGE, withMutable);
                m.insn(POP);
            }
            m.insn(ARETURN);
            m.visitLabel(next);
        }
        m.typeInsn(NEW, "java/lang/NoSuchFieldException");
        m.insn(DUP);
        m.load(1).visitInit("java/lang/NoSuchFieldException",
            "(Ljava/lang/String;)V");
        m.insn(ATHROW);
        if(withMutable != null) {
            m.visitLabel(withMutable);
            m.methodInsn(INVOKEINTERFACE, "ria/lang/Struct",
                "get", "(I)Ljava/lang/Object;");
            m.insn(ARETURN);
        }
        m.closeMethod();

        initBinders(context); // get/set accessors depend on it

        // get(int)
        m = st.newMethod(ACC_PUBLIC, "get", "(I)Ljava/lang/Object;");
        m.localVarCount = 2;
        m.load(0).varInsn(ILOAD, 1);
        jumps = new Label[fieldCount];
        int mutableCount = 0;
        for(i = 0; i < fieldCount; ++i) {
            jumps[i] = new Label();
            if(fields[i].mutable) {
                ++mutableCount;
            }
        }
        dflt = new Label();
        m.switchInsn(0, fieldCount - 1, dflt, null, jumps);
        if(withMutable != null) {
            withMutable = new Label();
        }
        for(i = 0; i < fieldCount; ++i) {
            field = fields[i];
            m.visitLabel(jumps[i]);
            if(field.property > 0) {
                new Apply(null, field.value,
                    new UnitConstant(null), field.line).gen(m);
            } else if(field.property < 0) {
                field.value.gen(m);
            } else {
                m.fieldInsn(GETFIELD, cn, field.javaName, "Ljava/lang/Object;");
            }
            if(field.inherited) {
                m.load(0).fieldInsn(GETFIELD, cn, "i" + i, "I");
                m.insn(DUP);
                m.jumpInsn(IFGE, withMutable);
                m.insn(POP);
            }
            m.insn(ARETURN);
        }
        m.visitLabel(dflt);
        m.insn(ACONST_NULL);
        m.insn(ARETURN);
        if(withMutable != null) {
            m.visitLabel(withMutable);
            m.methodInsn(INVOKEINTERFACE, "ria/lang/Struct",
                "get", "(I)Ljava/lang/Object;");
            m.insn(ARETURN);
        }
        m.closeMethod();

        // Object ref(int field, int[] idx, int at)
        if(withParent != null) {
            m = st.newMethod(ACC_PUBLIC, "ref", "(I[II)Ljava/lang/Object;");
            Label isConst = null;
            Label isVar = null;
            jumps = new Label[fieldCount];
            for(i = 0; i < fieldCount; ++i) {
                if(fields[i].inherited) {
                    jumps[i] = new Label();
                } else if(fields[i].mutable || fields[i].property > 0) {
                    if(isVar == null) {
                        isVar = new Label();
                    }
                    jumps[i] = isVar;
                } else {
                    if(isConst == null) {
                        isConst = new Label();
                    }
                    jumps[i] = isConst;
                }
            }
            dflt = new Label();
            next = new Label();
            m.load(0).load(2).varInsn(ILOAD, 3);
            m.varInsn(ILOAD, 1); // this idx at switch(field) { jumps }
            m.switchInsn(0, fieldCount - 1, dflt, null, jumps);
            int inheritedCount = 0;
            for(i = 0; i < fieldCount; ++i) {
                if(!fields[i].inherited) {
                    continue;
                }
                ++inheritedCount;
                m.visitLabel(jumps[i]);
                i_str = Integer.toString(i);
                m.load(0).fieldInsn(GETFIELD, cn, "i".concat(i_str), "I");
                m.insn(IASTORE);
                m.fieldInsn(GETFIELD, cn, fields[i].javaName,
                    "Ljava/lang/Object;");
                m.load(0).fieldInsn(GETFIELD, cn, "h".concat(i_str), "Z");
                m.jumpInsn(GOTO, next);
            }
            if(isVar != null) {
                m.visitLabel(isVar);
                m.varInsn(ILOAD, 1);
                m.insn(IASTORE);
                m.intConst(0);  // not hidden
                m.jumpInsn(GOTO, next);
            }
            if(isConst != null) {
                m.visitLabel(isConst);
                m.intConst(-1);
                m.insn(IASTORE);
                m.varInsn(ILOAD, 1);
                m.methodInsn(INVOKEVIRTUAL, cn, "get", "(I)Ljava/lang/Object;");
                m.intConst(0);  // not hidden
            }
            m.visitLabel(next); // ret idx[1]
            m.varInsn(ISTORE, 1);
            m.load(2).varInsn(ILOAD, 3);
            m.intConst(1);
            m.insn(IADD);
            m.varInsn(ILOAD, 1);
            m.insn(IASTORE);    // ret idx[1]
            m.insn(ARETURN);
            m.visitLabel(dflt);
            m.insn(ACONST_NULL);
            m.insn(ARETURN);
            m.closeMethod();

            // Build the mapping function from field number to name for inherited fields
            // String eqName(int field)
            if(inheritedCount > 0) {
                m = st.newMethod(ACC_PUBLIC, "eqName", "(I)Ljava/lang/String;");
                dflt = new Label();
                jumps = new Label[fieldCount];
                for(i = 0; i < fieldCount; ++i) {
                    jumps[i] = fields[i].inherited ? new Label() : dflt;
                }
                m.load(0).varInsn(ILOAD, 1); // this switch(field) { jumps }
                m.switchInsn(0, fieldCount - 1, dflt, null, jumps);
                Label check = new Label();
                for(i = 0; i < fieldCount; ++i) {
                    if(fields[i].inherited) {
                        m.visitLabel(jumps[i]);
                        m.fieldInsn(GETFIELD, cn,
                            "h".concat(Integer.toString(i)), "Z");
                        if(--inheritedCount <= 0) {
                            break;
                        }
                        m.jumpInsn(GOTO, check);
                    }
                }
                m.visitLabel(check);
                m.jumpInsn(IFEQ, next = new Label());
                m.ldcInsn("");
                m.insn(ARETURN);
                m.visitLabel(next);
                m.load(0).visitLabel(dflt);
                m.varInsn(ILOAD, 1);
                m.methodInsn(INVOKEVIRTUAL, cn,
                    "name", "(I)Ljava/lang/String;");
                m.insn(ARETURN);
                m.closeMethod();
            }
        }

        // Check for mutable fields and generate field setter if needed
        if(mutableCount == 0) {
            return cn;
        }
        // set(String, Object)
        m = st.newMethod(ACC_PUBLIC, "set", "(Ljava/lang/String;Ljava/lang/Object;)V");
        m.localVarCount = 3;
        m.load(0);
        for(i = 0; i < fieldCount; ++i) {
            field = fieldsOrigOrder[i];
            if(!field.mutable) {
                continue;
            }
            next = new Label();
            m.load(1).ldcInsn(field.name);
            m.jumpInsn(IF_ACMPNE, next);
            if(field.property != 0) {
                LoadVar var = new LoadVar();
                var.var = 2;
                new Apply(null, field.setter, var, field.line).gen(m);
                m.insn(POP2);
            } else if(field.inherited) {
                m.fieldInsn(GETFIELD, cn, field.javaName, "Ljava/lang/Object;");
                m.load(1).load(2)
                    .methodInsn(INVOKEINTERFACE, "ria/lang/Struct", "set",
                        "(Ljava/lang/String;Ljava/lang/Object;)V");
            } else {
                m.load(2).fieldInsn(PUTFIELD, cn, field.javaName, "Ljava/lang/Object;");
            }
            m.insn(RETURN);
            m.visitLabel(next);
        }
        m.insn(POP);
        m.insn(RETURN);
        m.closeMethod();
        return cn;
    }

    void genWith(Context context, Code src, Map<String, CType> srcFields) {
        srcFields = new HashMap<>(srcFields);
        for(int i = 0; i < fieldCount; ++i) {
            srcFields.remove(fields[i].name);
        }
        if(srcFields.isEmpty()) { // everything has been overridden
            gen(context);
            return;
        }
        mustGen = true;
        withParent = src;
        StructField[] fields = new StructField[fieldCount + srcFields.size()];
        String[] withFields = srcFields.keySet().toArray(new String[0]);
        Arrays.sort(withFields);
        for(int i = 0; i < withFields.length; ++i) {
            StructField sf = new StructField();
            sf.name = withFields[i];
            sf.inherited = true;
            // whether to generate the setter code
            sf.mutable = srcFields.get(sf.name).field == RiaType.FIELD_MUTABLE;
            fields[i] = sf;
        }
        this.withFields = new String[withFields.length + 1];
        System.arraycopy(withFields, 0, this.withFields, 1, withFields.length);
        System.arraycopy(this.fields, 0, fields, srcFields.size(), fieldCount);
        this.fields = fields;
        fieldCount = fields.length;
        close();
        gen(context);
    }

    private class Bind extends BindRef implements Binder, CaptureWrapper {
        private StructField field;
        private boolean fun; // is this binding a function?
        private boolean direct; // Direct access to the binding?
        private boolean mutable; // Is the binding mutable - if not, we can optimise a bit
        private int var;

        Bind(StructField sf) {
            type = sf.value.type;
            binder = this;
            mutable = sf.mutable;
            fun = sf.value instanceof Function;
            field = sf;
        }

        void initGen(Context context) {
            if(prepareConst(context)) {
                direct = true;
                field.binder = null;
                return;
            }

            if(!mutable && fun) {
                ((Function)field.value).prepareGen(context, false);
                context.varInsn(ASTORE, var = context.localVarCount++);
            } else {
                if(arrayVar == -1) {
                    arrayVar = context.localVarCount++;
                }
                var = arrayVar;
                field.binder = null;
            }
        }

        @Override
        public BindRef getRef(int line) {
            field.binder = this;
            return this;
        }

        @Override
        public CaptureWrapper capture() {
            return !fun || mutable ? this : null;
        }

        @Override
        public boolean flagop(int fl) {
            if((fl & ASSIGN) != 0) {
                return mutable;
            }
            if((fl & PURE) != 0) {
                return !mutable;
            }
            if((fl & DIRECT_BIND) != 0) {
                return direct;
            }
            if((fl & CONST) != 0) {
                return direct || !mutable && field.value.flagop(CONST);
            }
            return false;
        }

        @Override
        boolean prepareConst(Context context) {
            return direct || !mutable && field.value.prepareConst(context);
        }

        @Override
        public void gen(Context context) {
            if(direct) {
                field.value.gen(context);
            } else {
                context.load(var);
            }
        }

        @Override
        public void genPreGet(Context context) {
            if(direct) {
                context.insn(ACONST_NULL); // wtf
            } else {
                context.load(var);
            }
        }

        @Override
        public void genGet(Context context) {
            if(direct) {
                context.insn(POP);
                field.value.gen(context);
            } else if(impl == null) {
                // GenericStruct
                context.ldcInsn(field.name);
                context.methodInsn(INVOKEINTERFACE, "ria/lang/Struct", "get",
                    "(Ljava/lang/String;)Ljava/lang/Object;");
            } else if(field.property != 0) {
                // Property accessor
                context.intConst(field.index);
                context.methodInsn(INVOKEINTERFACE, "ria/lang/Struct",
                    "get", "(I)Ljava/lang/Object;");
            } else {
                context.fieldInsn(GETFIELD, impl, field.javaName,
                    "Ljava/lang/Object;");
            }
        }

        @Override
        public void genSet(Context context, Code value) {
            if(impl != null && field.property == 0) {
                value.gen(context);
                context.fieldInsn(PUTFIELD, impl, field.javaName,
                    "Ljava/lang/Object;");
                return;
            }
            context.ldcInsn(field.name);
            value.gen(context);
            context.methodInsn(INVOKEINTERFACE, "ria/lang/Struct", "set",
                "(Ljava/lang/String;Ljava/lang/Object;)V");
        }

        @Override
        public Object captureIdentity() {
            return direct ? null : StructConstructor.this;
        }

        @Override
        public String captureType() {
            return impl != null ? 'L' + impl + ';' : "Lria/lang/Struct;";
        }
    }
}

