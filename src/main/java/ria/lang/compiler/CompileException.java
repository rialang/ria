package ria.lang.compiler;

import ria.lang.compiler.nodes.Node;

public class CompileException extends RuntimeException {
    String fn;
    int line;
    int col;
    String what;
    Node cause;

    static String format(CType param1, CType param2,
                         String s, TypeException ex, Scope scope) {
        StringBuilder result = new StringBuilder();
        int p = 0, i;
        boolean msg = false;
        while ((i = s.indexOf('#', p)) >= 0 && i < s.length() - 1) {
            result.append(s, p, i);
            p = i + 2;
            switch (s.charAt(i + 1)) {
                case '0':
                    result.append(ex.getMessage(scope));
                    msg = true;
                    break;
                case '1': result.append(param1.toString(scope, ex)); break;
                case '2': result.append(param2.toString(scope, ex)); break;
                case '~':
                    result.append(param1.toString(scope, ex));
                    result.append(" is not ");
                    result.append(param2.toString(scope, ex));
                    break;
                default:
                    result.append('#');
                    --p;
            }
        }
        result.append(s.substring(p));
        if (!msg && ex != null && ex.special) {
            result.append(" (");
            result.append(ex.getMessage(scope));
            result.append(")");
        }
        return result.toString();
    }

    public CompileException(int line, int col, String what) {
        this.line = line;
        this.col = col;
        this.what = what;
    }

    CompileException(Node pos,
                     JavaClassNotFoundException ex) {
        this(ex, pos, "Unknown class: " + ex.getMessage());
    }

    public CompileException(Node pos, String what) {
        this(null, pos, what);
    }

    private CompileException(Throwable ex, Node pos, String what) {
        super(ex);
        if (pos != null) {
            line = pos.line;
            col = pos.col;
        }
        this.what = what;
        this.cause = pos;
    }

    CompileException(Node pos, Scope scope,
                     CType param1, CType param2, String what,
                     TypeException ex) {
        this(ex, pos, format(param1, param2, what, ex, scope));
    }

    @Override
    public String getMessage() {
        return (fn == null ? "" : fn + ":") +
               (line == 0 ? "" : line + (col > 0 ? ":" + col + ": " : ": ")) +
               what;
    }
}
