package ria.lang.compiler;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 * Encoding:
 *
 * 00 - format identifier
 * Follows type description
 * 00 XX XX - free type variable XXXX
 * XX, where XX is 01..08 -
 *      primitives (same as CType.type UNIT - MAP_MARKER)
 * 09 x.. y.. - Function x -> y
 * 0A e.. i.. t.. - MAP<e,i,t>
 * 0B <requiredMembers...> FF <allowedMembers...> FF - Struct
 * 0C <requiredMembers...> FF <allowedMembers...> FF - Variant
 * 0C F9 ... - Variant with FL_ANY_CASE flag
 * (0B | 0C) F9? F8 ... - Variant or struct with FL_FLEX_TYPEDEF flag
 * 0D XX XX <param...> FF - java type
 * 0E e.. FF - java array e[]
 * FA XX XX <parameters...> FF - opaque type instance (X is "module:name")
 * FB XX XX - non-free type variable XXXX
 * FC ... - mutable field type
 * FD ... - the following type variable is ORDERED
 * FE XX XX - reference to non-primitive type
 * Follows list of type definitions
 *  <typeDef name
 *   typeDef array of type descriptions (FF)
 *   ...>
 *  FF
 * Follows utf8 encoded direct field mapping.
 *  XX XX XX XX - length
 *  'F' fieldName 00 function-class 00
 *  'P' fieldName 00 - property (field mapping as null)
 */
class TypeAttribute extends Attribute {
    private static final byte END = -1;
    private static final byte REF = -2;
    private static final byte ORDERED = -3;
    private static final byte MUTABLE = -4;
    private static final byte TAINTED = -5;
    private static final byte OPAQUE = -6;
    private static final byte ANYCASE = -7;
    private static final byte SMART = -8;

    final ModuleType moduleType;
    final Compiler compiler;
    private ByteVector encoded;

    TypeAttribute(ModuleType mt, Compiler ctx) {
        super("RiaModuleType");
        moduleType = mt;
        compiler = ctx;
    }

    @Override
    protected Attribute read(ClassReader cr, int off, int len, char[] buf,
                             int codeOff, Label[] labels) {
        int hdr = 3;
        switch(cr.b[off]) {
            case 0:
                hdr = 1; // version 0 has only version in header
            case 1:
                break;
            default:
                throw new RuntimeException("Unknown type encoding: " + cr.b[off]);
        }
        DecodeType decoder = new DecodeType(cr, off + hdr, len - hdr, buf, compiler.opaqueTypes);
        CType t = decoder.read();
        Map<String, CType[]> typeDefs = decoder.readTypeDefs();
        return new TypeAttribute(new ModuleType(t, typeDefs, hdr != 1, -1),
            compiler);
    }

    @Override
    protected ByteVector write(ClassWriter cw, byte[] code, int len,
                               int maxStack, int maxLocals) {
        if(encoded != null) {
            return encoded;
        }
        EncodeType enc = new EncodeType();
        for(Map.Entry<String, CType[]> e : moduleType.typeDefs.entrySet()) {
            CType[] def = e.getValue();
            CType t = def[def.length - 1];
            if(t.type >= RiaType.OPAQUE_TYPES && t.requiredMembers == null) {
                enc.opaque.put(t.type,
                    moduleType.name + ':' + e.getKey());
            }
        }
        enc.cw = cw;
        enc.buf.putByte(1); // encoding version
        enc.buf.putShort(0);
        enc.write(moduleType.type);
        enc.writeTypeDefs(moduleType.typeDefs);
        return encoded = enc.buf;
    }

    private static final class EncodeType {
        ClassWriter cw;
        ByteVector buf = new ByteVector();
        Map<CType, Integer> refs = new HashMap<>();
        Map<CType, Integer> vars = new HashMap<>();
        Map<Integer, String> opaque = new HashMap<>();

        void writeMap(Map<String, CType> m) {
            if(m != null) {
                for(Map.Entry<String, CType> e : m.entrySet()) {
                    CType t = e.getValue();
                    if(t.field == RiaType.FIELD_MUTABLE) {
                        buf.putByte(MUTABLE);
                    }
                    write(t);
                    int name = cw.newUTF8(e.getKey());
                    buf.putShort(name);
                }
            }
            buf.putByte(END);
        }

        void writeArray(CType[] param) {
            for(CType aParam : param) {
                write(aParam);
            }
            buf.putByte(END);
        }

        void write(CType type) {
            type = type.deref();
            if(type.type == RiaType.VAR) {
                Integer id = vars.get(type);
                if(id == null) {
                    vars.put(type, id = vars.size());
                    if(id > 0x7fff) {
                        throw new RuntimeException("Too many type parameters");
                    }
                    if((type.flags & RiaType.FL_ORDERED_REQUIRED) != 0) {
                        buf.putByte(ORDERED);
                    }
                }
                buf.putByte((type.flags & RiaType.FL_TAINTED_VAR) == 0
                    ? RiaType.VAR : TAINTED);
                buf.putShort(id);
                return;
            }
            if(type.type < RiaType.PRIMITIVES.length &&
                RiaType.PRIMITIVES[type.type] != null) {
                // primitives
                buf.putByte(type.type);
                return;
            }
            Integer id = refs.get(type);
            if(id != null) {
                if(id > 0x7fff) {
                    throw new RuntimeException("Too many type parts");
                }
                buf.putByte(REF);
                buf.putShort(id);
                return;
            }
            refs.put(type, refs.size());
            if(type.type >= RiaType.OPAQUE_TYPES) {
                Object idstr = opaque.get(type.type);
                if(idstr == null) {
                    idstr = type.requiredMembers.keySet().toArray()[0];
                }
                buf.putByte(OPAQUE);
                buf.putShort(cw.newUTF8(idstr.toString()));
                writeArray(type.param);
                return;
            }
            buf.putByte(type.type);
            switch(type.type) {
                case RiaType.FUN:
                    write(type.param[0]);
                    write(type.param[1]);
                    break;
                case RiaType.MAP:
                    writeArray(type.param);
                    break;
                case RiaType.STRUCT:
                case RiaType.VARIANT:
                    if((type.allowedMembers == null || type.allowedMembers.isEmpty())
                        && (type.requiredMembers == null ||
                        type.requiredMembers.isEmpty())) {
                        throw new CompileException(0, 0,
                            type.type == RiaType.STRUCT
                                ? "Internal error: empty struct"
                                : "Internal error: empty variant");
                    }
                    if((type.flags & RiaType.FL_ANY_CASE) != 0) {
                        buf.putByte(ANYCASE);
                    }
                    if((type.flags & RiaType.FL_FLEX_TYPEDEF) != 0) {
                        buf.putByte(SMART);
                    }
                    writeMap(type.allowedMembers);
                    writeMap(type.requiredMembers);
                    break;
                case RiaType.JAVA:
                    buf.putShort(cw.newUTF8(type.javaType.description));
                    writeArray(type.param);
                    break;
                case RiaType.JAVA_ARRAY:
                    write(type.param[0]);
                    break;
                default:
                    throw new RuntimeException("Unknown type: " + type.type);
            }
        }

        void writeTypeDefs(Map<String, CType[]> typeDefs) {
            for(Map.Entry<String, CType[]> e : typeDefs.entrySet()) {
                buf.putShort(cw.newUTF8(e.getKey()));
                writeArray(e.getValue());
            }
            buf.putByte(END);
        }
    }

    private static final class DecodeType {
        private static final int VAR_DEPTH = 1;
        final ClassReader cr;
        final byte[] in;
        final char[] buf;
        final int end;
        final Map<Integer, CType> vars = new HashMap<>();
        final List<CType> refs = new ArrayList<>();
        final Map<String, CType> opaqueTypes;
        int p;

        DecodeType(ClassReader cr, int off, int len, char[] buf,
                   Map<String, CType> opaqueTypes) {
            this.cr = cr;
            in = cr.b;
            p = off;
            end = p + len;
            this.buf = buf;
            this.opaqueTypes = opaqueTypes;
        }

        Map<String, CType> readMap() {
            if(in[p] == END) {
                ++p;
                return null;
            }
            Map<String, CType> res = new HashMap<>();
            while(in[p] != END) {
                CType t = read();
                res.put(cr.readUTF8(p, buf), t);
                p += 2;
            }
            ++p;
            return res;
        }

        CType[] readArray() {
            List<CType> param = new ArrayList<>();
            while(in[p] != END) {
                param.add(read());
            }
            ++p;
            return param.toArray(new CType[0]);
        }

        CType read() {
            CType t;
            int tv;

            if(p >= end) {
                throw new RuntimeException("Invalid type description");
            }
            switch(tv = in[p++]) {
                case RiaType.VAR:
                case TAINTED: {
                    Integer var = cr.readUnsignedShort(p);
                    p += 2;
                    if((t = vars.get(var)) == null) {
                        vars.put(var, t = new CType(VAR_DEPTH));
                    }
                    if(tv == TAINTED) {
                        t.flags |= RiaType.FL_TAINTED_VAR;
                    }
                    return t;
                }
                case ORDERED:
                    t = read();
                    t.flags |= RiaType.FL_ORDERED_REQUIRED;
                    return t;
                case REF: {
                    int v = cr.readUnsignedShort(p);
                    p += 2;
                    if(refs.size() <= v) {
                        throw new RuntimeException("Illegal type reference");
                    }
                    return refs.get(v);
                }
                case MUTABLE:
                    return RiaType.fieldRef(1, read(), RiaType.FIELD_MUTABLE);
            }
            if(tv < RiaType.PRIMITIVES.length && tv > 0) {
                return RiaType.PRIMITIVES[tv];
            }
            t = new CType(tv, null);
            refs.add(t);
            if(t.type == RiaType.FUN) {
                t.param = new CType[2];
                t.param[0] = read();
                t.param[1] = read();
            } else if(tv == RiaType.MAP) {
                t.param = readArray();
            } else if(tv == RiaType.STRUCT || tv == RiaType.VARIANT) {
                if(in[p] == ANYCASE) {
                    t.flags |= RiaType.FL_ANY_CASE;
                    ++p;
                }
                if(in[p] == SMART) {
                    t.flags |= RiaType.FL_FLEX_TYPEDEF;
                    ++p;
                }
                t.allowedMembers = readMap();
                t.requiredMembers = readMap();
                Map<String, CType> param;
                if(t.allowedMembers == null) {
                    if((param = t.requiredMembers) == null) {
                        param = new HashMap<>();
                    }
                } else if(t.requiredMembers == null) {
                    param = t.allowedMembers;
                } else {
                    param = new HashMap<>(t.allowedMembers);
                    param.putAll(t.requiredMembers);
                }
                t.param = new CType[param.size() + 1];
                t.param[0] = new CType(VAR_DEPTH);
                Iterator<CType> i = param.values().iterator();
                for(int n = 1; i.hasNext(); ++n) {
                    t.param[n] = i.next();
                }
            } else if(tv == RiaType.JAVA) {
                t.javaType = JavaType.fromDescription(cr.readUTF8(p, buf));
                p += 2;
                t.param = readArray();
            } else if(tv == RiaType.JAVA_ARRAY) {
                t.param = new CType[]{read()};
            } else if(tv == OPAQUE) {
                String idstr = cr.readUTF8(p, buf);
                p += 2;
                synchronized(opaqueTypes) {
                    CType old = opaqueTypes.get(idstr);
                    if(old != null) {
                        t.type = old.type;
                    } else {
                        t.type = opaqueTypes.size() + RiaType.OPAQUE_TYPES;
                        opaqueTypes.put(idstr, t);
                    }
                }
                t.requiredMembers =
                    Collections.singletonMap(idstr, RiaType.NO_TYPE);
                t.param = readArray();
            } else {
                throw new RuntimeException("Unknown type id: " + tv);
            }
            return t;
        }

        Map<String, CType[]> readTypeDefs() {
            Map<String, CType[]> result = new HashMap<>();
            while(in[p] != END) {
                String name = cr.readUTF8(p, buf);
                p += 2;
                result.put(name, readArray());
            }
            ++p;
            return result;
        }
    }
}

