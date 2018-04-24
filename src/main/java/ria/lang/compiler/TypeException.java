package ria.lang.compiler;

import java.util.ArrayList;
import java.util.List;

public class TypeException extends Exception {
    boolean special;
    CType a, b;
    String sep, ext;
    List<Object> trace;

    TypeException(String what) {
        super(what);
        trace = new ArrayList<>();
    }

    TypeException(CType a_, CType b_) {
        a = a_;
        b = b_;
        sep = " is not ";
        ext = "";
        trace = new ArrayList<>();
    }

    TypeException(CType a_, String sep_, CType b_, String ext_) {
        a = a_;
        b = b_;
        sep = sep_;
        ext = ext_;
        trace = new ArrayList<>();
    }

    @Override
    public String getMessage() {
        return getMessage(null);
    }

    public String getMessage(Scope scope) {
        if(a == null) {
            return super.getMessage();
        }
        return "Type mismatch: " + a.toString(scope, null) +
            sep + b.toString(scope, null) + ext;
    }
}
