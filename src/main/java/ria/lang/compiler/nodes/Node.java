package ria.lang.compiler.nodes;

import ria.lang.compiler.CompileException;
import ria.lang.compiler.RiaParser;

public class Node {
    public int line;
    public int col;
    public String kind;

    public String str() {
        return toString();
    }

    public Node pos(int line, int col) {
        this.line = line;
        this.col = col;
        return this;
    }

    public String toString() {
        char[] s = RiaParser.currentSrc.get();
        if(s == null) {
            return getClass().getName();
        }
        int p = 0, l = line;
        if(--l > 0) {
            while(p < s.length && (s[p++] != '\n' || --l > 0)) {
            }
        }
        p += col - 1;
        if(p < 0) {
            p = 0;
        }
        int e = p;
        char c;
        while(++e < s.length && ((c = s[e]) > ' ' && c != ':' &&
            c != ';' && c != '.' && c != ',' && c != '(' && c != ')' &&
            c != '[' && c != ']' && c != '{' && c != '}')) {
        }
        return '`' + new String(s, p, Math.min(e, s.length) - p) + '\'';
    }

    public String sym() {
        throw new CompileException(this, "Expected symbol here, not " + this);
    }
}
