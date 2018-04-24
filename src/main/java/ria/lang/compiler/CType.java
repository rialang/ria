package ria.lang.compiler;

import java.util.Map;

public class CType {
    public int type;
    Map<String, CType> requiredMembers;
    public Map<String, CType> allowedMembers;

    public CType[] param;
    CType ref;
    int depth;
    int flags;
    public int field;
    boolean seen;

    Object doc;
    public JavaType javaType;

    CType(int depth) {
        this.depth = depth;
    }

    public CType(int type, CType[] param) {
        this.type = type;
        this.param = param;
    }

    CType(String javaSig) {
        type = RiaType.JAVA;
        this.javaType = JavaType.fromDescription(javaSig);
        param = RiaType.NO_PARAM;
    }

    public String toString() {
        return (String)new ShowTypeFun().apply("",
            TypeDescription.riaType(this, null, null));
    }

    public String toString(Scope scope, TypeException ex) {
        return (String)new ShowTypeFun().apply("",
            TypeDescription.riaType(this, TypePattern.toPattern(scope, false), ex));
    }

    public CType deref() {
        CType res = this;
        while(res.ref != null) {
            res = res.ref;
        }
        for(CType next, type = this; type.ref != null; type = next) {
            next = type.ref;
            type.ref = res;
        }
        if((res.type <= 0 || res.type > RiaType.PRIMITIVE_END) &&
            res.doc == null) {
            res.doc = this;
        }
        return res;
    }

    String doc() {
        for(CType t = this; t != null; t = t.ref) {
            if(t.doc != null) {
                String doc;
                if(t.doc instanceof CType) {
                    CType ref = (CType)t.doc;
                    t.doc = null;
                    doc = ref.doc();
                } else {
                    doc = (String)t.doc;
                }
                if(doc != null) {
                    if((doc = doc.trim()).length() != 0) {
                        t.doc = doc;
                        return doc;
                    }
                    t.doc = null;
                }
            }
        }
        return null;
    }
}
