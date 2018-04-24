package ria.lang.compiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DescribeContext {
    TypePattern defs;
    Map<CType, String> vars = new HashMap<>();
    Map<CType, TypeDescription> refs = new HashMap<>();
    List<Object> trace;

    String getVarName(CType t) {
        String v = vars.get(t);
        if(v == null) {
            // 26^7 > 2^32, should be enough ;)
            char[] buf = new char[10];
            int p = buf.length;
            if((t.flags & RiaType.FL_ERROR_IS_HERE) != 0) {
                buf[--p] = '*';
            }
            int n = vars.size() + 1;
            while(n > 26) {
                buf[--p] = (char)('a' + n % 26);
                n /= 26;
            }
            buf[--p] = (char)(96 + n);
            if((t.flags & RiaType.FL_TAINTED_VAR) != 0) {
                buf[--p] = '_';
            }
            buf[--p] =
                (t.flags & RiaType.FL_ORDERED_REQUIRED) == 0 ? '`' : '^';
            v = new String(buf, p, buf.length - p);
            vars.put(t, v);
        }
        return v;
    }
}
