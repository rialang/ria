package ria.lang.compiler;

import ria.lang.Core;
import ria.lang.compiler.nodes.BinOp;
import ria.lang.compiler.nodes.Bind;
import ria.lang.compiler.nodes.Eof;
import ria.lang.compiler.nodes.InstanceOf;
import ria.lang.compiler.nodes.IsOp;
import ria.lang.compiler.nodes.Node;
import ria.lang.compiler.nodes.NumLit;
import ria.lang.compiler.nodes.ObjectRefOp;
import ria.lang.compiler.nodes.ParseExpr;
import ria.lang.compiler.nodes.Seq;
import ria.lang.compiler.nodes.Str;
import ria.lang.compiler.nodes.Sym;
import ria.lang.compiler.nodes.TypeDef;
import ria.lang.compiler.nodes.TypeNode;
import ria.lang.compiler.nodes.TypeOp;
import ria.lang.compiler.nodes.XNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RiaParser {

    public static final ThreadLocal<char[]> currentSrc = new ThreadLocal<>();
    public static final String FIELD_OP = ".fieldop.";

    private static final char[] CHS = (
        "                                " + // 0x
            // !"#$%&'()*+,-./0123456789:;<=>?
            " .'.x..x  .. ../xxxxxxxxxx. ...x" + // 2x
            //`abcdefghijklmnopqrstuvwxyz{|}~
            ".xxxxxxxxxxxxxxxxxxxxxxxxxx[ ].x" + // 4x
            "`xxxxxxxxxxxxxxxxxxxxxxxxxx . . ").toCharArray();

    private static final String[][] OPS = {
        {"*", "/", "%"},
        {"+", "-"},
        {null}, // non-standard operators
        {"."},
        {"<", ">", "<=", ">=", "==", "!=", "=~", "!~"},
        {null}, // not
        {null}, // and or
        {"^"},
        {":+", ":.", "++"},
        {"|>"},
        {"is"},
        {":="},
        {null}, // loop
    };
    private static final int FIRST_OP_LEVEL = 3;
    public static final int IS_OP_LEVEL = opLevel("is");
    public static final int COMP_OP_LEVEL = opLevel("<");
    public static final int NOT_OP_LEVEL = COMP_OP_LEVEL + 1;
    private static final int LIST_OP_LEVEL = NOT_OP_LEVEL + 3;
    private static final Eof EOF = new Eof("EOF");
    private static final String EXPECT_DEF = "Expected field or method definition, found";

    // ugly all-in-one type expression parser
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_FUNRET = 1;
    private static final int TYPE_VARIANT = 2;
    private static final int TYPE_VARIANT_ARG = 3;
    XNode loads;
    String sourceName;
    String moduleName;
    int moduleNameLine;
    String topDoc;
    boolean isModule;
    boolean deprecated;
    private char[] src;
    private int p;
    private Node eofWas;
    private int flags;
    private int line = 1;
    private int lineStart;
    private String riaDocStr;
    private boolean riaDocReset;

    RiaParser(String sourceName, char[] src, int flags) {
        this.sourceName = sourceName;
        this.src = src;
        this.flags = flags;
    }

    private static int opLevel(String op) {
        int i = 0;
        while(!Objects.equals(OPS[i][0], op)) {
            ++i;
        }
        return i + FIRST_OP_LEVEL;
    }

    int currentLine() {
        return line;
    }

    private int directive(int from, int to) {
        boolean doc = src[from] != '%';
        if(doc && (flags & Compiler.GF_DOC) == 0) {
            return to;
        }
        ++from;
        String str = new String(src, from, to - from);
        if(doc) {
            riaDocStr = riaDocStr == null || riaDocReset
                ? str : riaDocStr + '\n' + str;
            riaDocReset = false;
        } else if(str.length() >= 2) {
            // Reserved for future use "%directive params"
            throw new CompileException(line, from - lineStart, "Directives are not yet supported");
        }
        return to;
    }

    private int skipSpace() {
        char[] src = this.src;
        int i = p, sp;
        char c;
        riaDocReset = true;
        for(; ; ) {
            while(i < src.length &&
                ((c = src[i]) >= '\000' && c <= ' ' || c == 0xa0)) {
                ++i;
                if(c == '\n') {
                    ++line;
                    lineStart = i;
                }
            }
            if(i + 1 < src.length && src[i] == '/') {
                if(src[i + 1] == '/') {
                    sp = i += 2;
                    while(i < src.length && src[i] != '\n' && src[i] != '\r') {
                        ++i;
                    }
                    if(i > sp && (src[sp] == '/' || src[sp] == '%')) {
                        i = directive(sp, i);
                    }
                    continue;
                }
                if(src[i + 1] == '*') {
                    int l = line, col = i - lineStart + 1;
                    sp = i += 2;
                    for(int level = 1; level > 0; ) {
                        if(++i >= src.length) {
                            throw new CompileException(l, col, "Unclosed /* comment");
                        }
                        if((c = src[i - 1]) == '\n') {
                            ++line;
                            lineStart = i - 1;
                        } else if(c == '*' && src[i] == '/') {
                            ++i;
                            --level;
                        } else if(c == '/' && src[i] == '*') {
                            ++i;
                            ++level;
                        }
                    }
                    if(i - 3 > sp && src[sp] == '*') {
                        directive(sp, i - 2);
                    }
                    continue;
                }
            }
            return i;
        }
    }

    private Node fetch() {
        int i = skipSpace();
        if(i >= src.length) {
            return EOF;
        }
        char[] src = this.src;
        char c;
        p = i + 1;
        int line = this.line, col = p - lineStart;

        // Easy cases first... well apart from the dots,
        // which is needed to differentiate between compose operator and struct field access
        switch(src[i]) {
            case '.':
                // Check for "." operator being used to compose functions
                if((i <= 0 || (c = src[i - 1]) < '~' && CHS[c] == ' ' &&
                    (i + 1 >= src.length || (c = src[i + 1]) < '~' && CHS[c] == ' '))) {
                    return new BinOp(".", COMP_OP_LEVEL - 1, true).pos(line, col);
                }
                break;
            case ';':
                return new XNode(";").pos(line, col);
            case ',':
                return new XNode(",").pos(line, col);
            case '(':
                return readSeq(')', null);
            case '[':
                return readList().pos(line, col);
            case '{':
                return XNode.struct(readMany(",", '}')).pos(line, col);
            case ')':
                p = i;
                return new Eof(")").pos(line, col);
            case ']':
                p = i;
                return new Eof("]").pos(line, col);
            case '}':
                p = i;
                return new Eof("}").pos(line, col);
            case '"':
                return readStr().pos(line, col);
            case '\'':
                return readAStr().pos(line, col);
            case '$':
                return new BinOp("$", 1, false).pos(line, col);
        }

        // Try to parse operators
        p = i;
        while(i < src.length && (c = src[i]) <= '~' &&
            (CHS[c] == '.' || c == '$' ||
                c == '/' && (i + 1 >= src.length ||
                    (c = src[i + 1]) != '/' && c != '*'))) {
            ++i;
        }
        if(i != p) {
            String s = new String(src, p, i - p);
            p = i;
            if(Objects.equals(s, "=") || Objects.equals(s, ":")) {
                return new XNode(s).pos(line, col);
            }
            if(Objects.equals(s, ".")) // Check for field access
            {
                return new BinOp(FIELD_OP, 0, true).pos(line, col);
            }
            if(Objects.equals(s, "::")) {
                return readObjectRef().pos(line, col);
            }
            for(i = OPS.length; --i >= 0; ) {
                for(int j = OPS[i].length; --j >= 0; ) {
                    if(Objects.equals(OPS[i][j], s)) {
                        return new BinOp(s, i + FIRST_OP_LEVEL, i != LIST_OP_LEVEL - FIRST_OP_LEVEL)
                            .pos(line, col);
                    }
                }
            }
            if(Objects.equals(s, "->")) {
                return new BinOp("->", 0, true).pos(line, col);
            }
            // Special type operator
            // TODO: Shall we keep this?
            if(Objects.equals(s, "<:")) {
                TypeNode t = readType(TYPE_NORMAL);
                if(t == null) {
                    throw new CompileException(line, col, "Expecting type expression");
                }
                return (Objects.equals(s, "<:") ? new IsOp(t) : new TypeOp(s, t))
                    .pos(line, col);
            }
            return new BinOp(s, FIRST_OP_LEVEL + 2, true).pos(line, col);
        }

        // Now try parsing number literals
        if((c = src[i]) >= '0' && c <= '9') {
            while(++i < src.length && ((c = src[i]) <= 'z' &&
                (CHS[c] == 'x' ||
                    c == '.' && (i + 1 >= src.length || src[i + 1] != '.')
                    || ((c == '+' || c == '-') &&
                    (src[i - 1] == 'e' || src[i - 1] == 'E'))))) {
            }
            String s = new String(src, p, i - p);
            p = i;
            try {
                return new NumLit(Core.parseNum(s)).pos(line, col);
            } catch(Exception e) {
                throw new CompileException(line, col, "Bad number literal '" + s + "'");
            }
        }

        // We probably have a symbol of some kind, so find the end of it
        while(++i < src.length && ((c = src[i]) > '~' || CHS[c] == 'x')) {
        }
        String s = new String(src, p, i - p);
        p = i;

        Node res;

        // Check for keywords
        switch(s) {
            case "if":
                res = readIf();
                break;
            case "do":
                res = readDo();
                break;
            case "and":
            case "or":
                res = new BinOp(s, NOT_OP_LEVEL + 1, true);
                break;
            case "not":
                res = new BinOp(s, NOT_OP_LEVEL, true);
                break;
            case "then":
            case "elif":
            case "else":
            case "of":
            case "done":
            case "catch":
            case "finally":
            case "end":
                res = new Eof(s);
                break;
            case "case":
                res = readCase();
                break;
            case "in":
                res = new BinOp(s, COMP_OP_LEVEL, true);
                break;
            case "div":
            case "shr":
            case "shl":
            case "b_and":
            case "with":
                res = new BinOp(s, FIRST_OP_LEVEL, true);
                break;
            case "b_or":
            case "xor":
                res = new BinOp(s, FIRST_OP_LEVEL + 1, true);
                break;
            case "is":
            case "cast":
            case "as":
                TypeNode t = readType(TYPE_NORMAL);
                if(t == null) {
                    throw new CompileException(line, col, "Expecting type expression");
                }
                return (Objects.equals(s, "is") ? new IsOp(t) : new TypeOp(s, t))
                    .pos(line, col);
            case "new":
                res = readNew();
                break;
            case "var":
            case "let":
            case "unbind":
            case "fall":
                res = new XNode(s);
                break;
            case "loop":
                res = new BinOp(s, IS_OP_LEVEL + 2, false);
                break;
            case "import":
                res = readImport();
                break;
            case "load":
                res = loads = new XNode(s, new Node[] {
                    readDotted("Expected module name after 'load', not a "),
                    loads
                });
                break;
            case "classOf":
                res = new XNode(s, readDottedType("Expected class name, not a "));
                break;
            case "typedef":
                res = readTypeDef();
                break;
            case "try":
                res = readTry();
                break;
            case "instanceof":
                res = new InstanceOf(readDotted("Expected class name, not a ").sym);
                break;
            case "class":
                res = readClassDef();
                break;
            default:
                if(s.charAt(0) != '`') {
                    res = new Sym(s); // normal identifier
                } else if(p >= src.length || src[p] != '`') {
                    throw new CompileException(line, col, "Syntax error"); // Invalid quoted symbol
                } else if(s.length() == 1) {
                    // Check for double quoted identifier
                    do {
                        if(++p >= src.length || src[p] == '\n') {
                            throw new CompileException(line, col, "Unterminated ``identifier");
                        }
                    } while(src[p - 1] != '`' || src[p] != '`');
                    // Strip off the `` from the identifier
                    s = new String(src, i + 1, p - i - 2);
                    res = new XNode("``", new Sym(s));
                    ++p;
                } else {
                    ++p; // swallow closing ` and return quoted identifier as an operator
                    res = new BinOp(s.substring(1), FIRST_OP_LEVEL + 2, true);
                }
                break;
        }
        return res.pos(line, col);
    }

    private Node readList() {
        char c;
        int i = p;
        if(i + 1 < src.length && src[i] == ':' && src[i + 1] == ']') {
            p = i + 2;
            return new XNode("list");
        }
        Node[] elem = readMany(",", ']');
        if(elem.length != 1 || i <= 1 ||
            (c = src[i - 2]) < '~' && CHS[c] == ' ' && c != ')' ||
            elem[0] instanceof BinOp && Objects.equals(((BinOp)elem[0]).op, "..")) {
            return new XNode("list", elem);
        }
        Node res = new ObjectRefOp(null, elem);
        res.kind = "listop";
        return res;
    }

    private Node def(List<Node> args, List<Node> expr, boolean structDef, String doc) {
        BinOp partial = null;
        String s = null;
        int i = 0, cnt = expr.size();
        if(cnt > 0) {
            Node o = expr.get(0);
            // We exclude some operators from partial binding
            // '-' is excluded so that we can parse -2 as a negative number
            // '$' is used to denote a lambda, so has no partial
            // '::' is field or method access, so cannot be partial either
            // 'not' is unary, and hence has no partial either
            if(o instanceof BinOp
                && (partial = (BinOp)o).parent == null
                && !Objects.equals(partial.op, "$") && !Objects.equals(partial.op, "-")
                && !Objects.equals(partial.op, "not") && !Objects.equals(partial.op, "::")) {
                s = partial.op;
                i = 1;
            } else if((o = expr.get(cnt - 1)) instanceof BinOp &&
                (partial = (BinOp)o).parent == null &&
                !partial.postfix) {
                if(Objects.equals(partial.op, "loop")) {
                    partial.postfix = true;
                } else {
                    s = partial.op;
                    --cnt;
                }
                if(Objects.equals(s, FIELD_OP)) {
                    throw new CompileException(partial, "Unexpected '.' here. Add space before it, if you want a compose section.");
                }
            }
        }
        if(s != null && i >= cnt) {
            if(Objects.equals(s, "loop") || Objects.equals(s, "with") || partial instanceof IsOp) {
                throw new CompileException(partial, "Special operator `" + s + "` cannot be used as a function");
            }
            if(partial instanceof TypeOp) {
                partial.right = new Sym(partial.hashCode() + partial.op);
                partial.right.pos(partial.line, partial.col);
                return XNode.lambda(partial.right, partial, null);
            }
            return new Sym(s).pos(partial.line, partial.col);
        }
        ParseExpr parseExpr = new ParseExpr();
        while(i < cnt) {
            parseExpr.add(expr.get(i++));
        }
        Node e = parseExpr.result();
        if(s != null) {
            if(cnt < expr.size()) {
                BinOp r = new BinOp("", 2, true);
                r.parent = r;
                r.right = e;
                r.left = e = new Sym(s);
                e.line = partial.line;
                e.col = partial.col;
                e = r;
            } else if(Objects.equals(s, "with")) { // handle using 'with' as right partial
                // TODO: Still experimental, although it seems to work
                // TODO: Need some more tests to confirm before we merge to master
                partial.right = new Sym(partial.hashCode() + s);
                partial.right.pos(partial.line, partial.col);
                BinOp bp = new BinOp("with", FIRST_OP_LEVEL, true);
                bp.left = partial.right;
                bp.right = parseExpr.result();
                e = XNode.lambda(partial.right, bp, null);
            } else {
                e = new XNode("rsection", new Node[] {new Sym(s), parseExpr.result()});
            }
            e.line = partial.line;
            e.col = partial.col;
        }
        Bind bind;
        return args == null ? e :
            args.size() == 1 && Objects.equals(args.get(0).kind, "struct")
                ? new XNode("struct-bind",
                new Node[] {args.get(0), e})
                : !Objects.equals((bind = new Bind(args, e, structDef, doc)).name, "_") ? bind
                : Objects.equals(bind.expr.kind, "lambda")
                ? bind.expr : new XNode("_", bind.expr);
    }

    private Node[] readArgs() {
        if((p = skipSpace()) >= src.length || src[p] != '(') {
            return null;
        }
        ++p;
        return readMany(",", ')');
    }

    // new ClassName(...)
    private Node readNew() {
        Node[] args = null;
        StringBuilder name = new StringBuilder();
        int dimensions = 0;
        while(args == null) {
            int nline = line, ncol = p - lineStart + 1;
            Node sym = fetch();
            if(!(sym instanceof Sym)) {
                throw new CompileException(nline, ncol, "Expecting class name after new");
            }
            name.append(((Sym)sym).sym);
            args = readArgs();

            // If we didn't get an argument list in '()' then try see if it is an array
            if(args == null) {
                char c = p >= src.length ? '\000' : src[p];
                if(c == '[') {
                    ++p;
                    args = new Node[] {readSeq(']', null)};
                    while(p + 1 < src.length &&
                        src[p] == '[' && src[p + 1] == ']') {
                        p += 2;
                        ++dimensions;
                    }
                    ++dimensions;
                    break;
                }
                // Check for further components of the name and add to the current name
                if(c == '.' || c == '$') {
                    ++p;
                    name.append(c == '.' ? '/' : c);
                } else {
                    throw new CompileException(line, p - lineStart + 1, "Expecting constructor argument list");
                }
            }
        }
        Node[] ex = new Node[args.length + 1];
        for(int i = 0; i < dimensions; ++i) {
            name.append("[]");
        }
        ex[0] = new Sym(name.toString());
        System.arraycopy(args, 0, ex, 1, args.length);
        return new XNode(dimensions == 0 ? "new" : "new-array", ex);
    }

    // ::something or ::something(...)
    private Node readObjectRef() {
        int nline = line, ncol = p - lineStart + 1;
        int st = skipSpace(), i = st;
        while(i < src.length && Character.isJavaIdentifierPart(src[i])) {
            ++i;
        }
        if(i == st) {
            throw new CompileException(nline, ncol, "Expecting java identifier after ::");
        }
        p = i;
        return new ObjectRefOp(new String(src, st, i - st),
            i < src.length && src[i] == '(' ? readArgs() : null);
    }

    private Node readIf() {
        Node cond = readSeqTo("then");
        Node expr = readSeq(' ', null);
        Node els;
        if(Objects.equals(eofWas.kind, "elif")) {
            els = readIf();
        } else {
            if(Objects.equals(eofWas.kind, "else")) {
                if(src.length > p && src[p] == ':') {
                    ++p;
                    List<Node> l = new ArrayList<>();
                    while(!((els = fetch()) instanceof Eof) && !Objects.equals(els.kind, ";")) {
                        l.add(els);
                    }
                    if(l.size() == 0) {
                        throw new CompileException(els, "Unexpected " + els);
                    }
                    if(Objects.equals(els.kind, ";") ||
                        !Objects.equals(els.kind, "EOF") && els.kind.length() > 1) {
                        p -= els.kind.length();
                    }
                    els = def(null, l, false, null);
                    eofWas = null;
                } else {
                    els = readSeq(' ', null);
                }
            } else {
                els = eofWas;
            }
            if(eofWas != null && !Objects.equals(eofWas.kind, "end")) {
                throw new CompileException(eofWas, "Expected end, found " + eofWas);
            }
        }
        return new XNode("if", new Node[] {cond, expr, els});
    }

    private void addCase(List<Node> cases, XNode choice, List<Node> expr) {
        if(expr.size() == 0) {
            throw new CompileException(choice, "Missing expression");
        }
        Node code;
        if(expr.size() == 1) {
            code = expr.get(0);
        } else {
            code = new Seq(expr.toArray(new Node[0]), null).pos(choice.line, choice.col);
        }
        choice.expr = new Node[] {choice.expr[0], code};
        cases.add(choice);
    }

    private Node readCase() {
        Node val = readSeqTo("of");
        Node[] statements = readMany(";", ' ');
        if(!Objects.equals(eofWas.kind, "end")) {
            throw new CompileException(eofWas, "Expected end, found " + eofWas);
        }
        List<Node> cases = new ArrayList<>(statements.length + 1);
        cases.add(val);
        XNode pattern = null;
        List<Node> expr = new ArrayList<>();
        for(int i = 0; i < statements.length; ++i) {
            if(Objects.equals(statements[i].kind, ":")) {
                if(pattern != null) {
                    addCase(cases, pattern, expr);
                    expr.clear();
                }
                pattern = (XNode)statements[i];
            } else if(statements[i] instanceof Sym && Objects.equals(statements[i].sym(), "...")) {
                if(i == 0 || i != statements.length - 1) {
                    throw new CompileException(statements[i], "Unexpected ...");
                }
                addCase(cases, pattern, expr);
                pattern = null;
                cases.add(new XNode("...", statements[i]));
            } else if(pattern != null) {
                expr.add(statements[i]);
            } else {
                throw new CompileException(statements[i], "Expecting option, not a " + statements[i]);
            }
        }
        if(pattern != null) {
            addCase(cases, pattern, expr);
        }
        return new XNode("case-of",
            cases.toArray(new Node[0]));
    }

    private Node readTry() {
        List<Node> catches = new ArrayList<>();
        catches.add(readSeq(' ', null));
        while(!Objects.equals(eofWas.kind, "finally") && !Objects.equals(eofWas.kind, "end")) {
            if(!Objects.equals(eofWas.kind, "catch")) {
                throw new CompileException(eofWas, "Expected finally or end, found " + eofWas);
            }
            XNode c = (XNode)eofWas;
            catches.add(c);
            c.expr = new Node[3];
            c.expr[0] = readDotted("Expected exception name, not ");
            Node n = fetch();
            if(n instanceof Sym) {
                c.expr[1] = n;
                n = fetch();
            }
            if(!Objects.equals(n.kind, ":")) {
                throw new CompileException(n, "Expected ':'" +
                    (c.expr[1] == null ? " or identifier" : "") +
                    ", but found " + n);
            }
            if(c.expr[1] == null) {
                c.expr[1] = new Sym("_").pos(n.line, n.col);
            }
            c.expr[2] = readSeq(' ', null);
        }
        if(!Objects.equals(eofWas.kind, "end")) {
            catches.add(readSeqTo("end"));
        }
        Node[] expr = catches.toArray(new Node[0]);
        if(expr.length <= 1) {
            throw new CompileException(eofWas, "try block must contain at least one catch or finally");
        }
        return new XNode("try", expr);
    }

    private Sym readDottedType(String what) {
        Sym t = readDotted(what);
        int s = p;
        while(src.length > p + 1 && src[p] == '[' && src[p + 1] == ']') {
            p += 2;
        }
        if(s != p) {
            t.sym = t.sym.concat(new String(src, s, p - s));
        }
        return t;
    }

    private Node readArgDefs() {
        int line_ = line, col_ = p++ - lineStart + 1;
        List<Node> args = new ArrayList<>();
        while((p = skipSpace()) < src.length && src[p] != ')') {
            if(args.size() != 0 && src[p++] != ',') {
                throw new CompileException(line, p - lineStart, "Expecting , or )");
            }
            args.add(readDottedType("Expected argument type, found "));
            Node name = fetch();
            if(!(name instanceof Sym)) {
                throw new CompileException(name, "Expected an argument name, found " + name);
            }
            args.add(name);
        }
        ++p;
        return new XNode("argument-list", args.toArray(new Node[0])).pos(line_, col_);
    }

    // Read the definition of a class
    private Node readClassDef() {
        List<Node> defs = new ArrayList<>();
        Node node = fetch();
        if(!(node instanceof Sym)) {
            throw new CompileException(node, "Expected a class name, found " + node);
        }
        p = skipSpace();
        defs.add(node);
        defs.add(p < src.length && src[p] == '(' ? readArgDefs()
            : new XNode("argument-list", new Node[0]));
        riaDocStr = null;
        List<Node> l = new ArrayList<>();
        node = readDottedType("Expected extends, field or method definition, found ");
        Node epos = node;
        if(Objects.equals(node.sym(), "extends")) {
            do {
                l.add(readDotted("Expected a class name, found "));
                int line_ = line, col_ = p - lineStart + 1;
                l.add(new XNode("arguments", readArgs()).pos(line_, col_));
            } while((p = skipSpace()) < src.length && src[p++] == ',');
            --p;
            node = readDottedType(EXPECT_DEF);
        }
        defs.add(new XNode("extends", l.toArray(new Node[0])).pos(epos.line, epos.col));
        l.clear();
        eofWas = node;
        collect:
        while(!(Objects.equals(eofWas.kind, "end") || eofWas instanceof Sym && Objects.equals(((Sym)eofWas).sym, "end"))) {
            if(node == null) {
                node = readDottedType(EXPECT_DEF);
            }
            String vsym = node.sym();
            if(Objects.equals(vsym, "var") || Objects.equals(vsym, "unbind") || Objects.equals(vsym, "let")) {
                l.add(new XNode(vsym).pos(node.line, node.col));
                node = fetch();
            }
            String doc = riaDocStr;
            String meth = "method";
            Node args = null;
            while(node instanceof Sym && !(Objects.equals(((Sym)node).sym, "end"))) {
                p = skipSpace();
                if(p < src.length && src[p] == '(') {
                    if(Objects.equals(meth, "error")) {
                        throw new CompileException(line, p - lineStart + 1, "Static method cannot be abstract");
                    }
                    if(!Objects.equals(meth, "method")) {
                        l.remove(0);
                    }
                    if(l.size() == 0) {
                        throw new CompileException(line, p - lineStart + 1, "Expected method name, found (");
                    }
                    if(l.size() == 1) {
                        args = readArgDefs();
                    }
                    break;
                }
                if(Objects.equals(((Sym)node).sym, "end") && l.size() == 0) {
                    break collect;
                }
                l.add(node);
                String s;
                // Check for static and abstract method
                if(Objects.equals(s = node.sym(), "static") || Objects.equals(s, "abstract")) {
                    meth = !Objects.equals(meth, "method") ? "error" :
                        Objects.equals(s, "static") ? "static-method" : "abstract-method";
                    node = readDottedType(EXPECT_DEF);
                } else {
                    node = fetch();
                }
            }
            if(args == null) {
                if(node instanceof IsOp) {
                    l.add(node);
                    node = fetch();
                }
                if(!Objects.equals(node.kind, "=")) {
                    throw new CompileException(node, "Expected '=' or argument list, found " + node);
                }
            }
            Node expr;
            if(Objects.equals(meth, "abstract-method")) {
                expr = null;
                eofWas = fetch();
            } else {
                expr = readSeq('e', null);
            }
            if(!Objects.equals(eofWas.kind, ",") && !Objects.equals(eofWas.kind, "end") && (!(eofWas instanceof Sym) ||
                !Objects.equals(((Sym)eofWas).sym, "end"))) {
                throw new CompileException(eofWas, "Unexpected " + eofWas);
            }
            if(args == null) {
                defs.add(new Bind(l, expr, false, doc));
            } else {
                Node[] m = expr != null
                    ? new Node[] {l.get(0), node, args, expr}
                    : new Node[] {l.get(0), node, args};
                defs.add(new XNode(meth, m).pos(node.line, node.col));
            }
            l.clear();
            node = null;
            riaDocStr = null;
        }
        return new XNode("class",
            defs.toArray(new Node[0]));
    }

    private Node readDo() {
        for(List<Node> args = new ArrayList<>(); ; ) {
            Node arg = fetch();
            if(arg instanceof Eof) {
                throw new CompileException(arg, "Unexpected " + arg);
            }
            if(Objects.equals(arg.kind, ":")) {
                Node expr = readSeqTo("done");
                if(args.isEmpty()) {
                    return XNode.lambda(new Sym("_").pos(arg.line, arg.col),
                        expr, null);
                }
                for(int i = args.size(); --i >= 0; ) {
                    expr = XNode.lambda(args.get(i), expr, null);
                }
                return expr;
            }
            args.add(arg);
        }
    }

    private Sym readDotted(String err) {
        Node first = fetch();
        StringBuilder result = new StringBuilder();
        for(Node n = first; ; n = fetch()) {
            if(!(n instanceof Sym)) {
                if(!Objects.equals(n.kind, "var") && !Objects.equals(n.kind, "let") && !Objects.equals(n.kind, "end")) {
                    throw new CompileException(n, err + n);
                }
                result.append(n.kind);
            } else {
                result.append(((Sym)n).sym);
            }
            p = skipSpace();
            if(p >= src.length || src[p] != '.') {
                break;
            }
            ++p;
            result.append("/");
        }
        Sym sym = new Sym(result.toString());
        sym.pos(first.line, first.col);
        return sym;
    }

    private XNode readImport() {
        Sym s = readDotted("Expected class path after 'import', not a ");
        ArrayList<Sym> imports = null;
        for(char c = ':'; ((p = skipSpace()) < src.length &&
            src[p] == c); c = ',') {
            ++p;
            if(imports == null) {
                imports = new ArrayList<>();
            }
            imports.add(new Sym(s.sym + '/' + fetch().sym()));
        }
        return imports == null ? new XNode("import", s) :
            new XNode("import", imports.toArray(new Node[0]));
    }

    private Node[] readMany(String sep, char end) {
        List<Node> res = new ArrayList<>();
        List<Node> args = null;
        List<Node> l = new ArrayList<>();
        String doc = null;
        // TODO: check for (something=) error
        Node sym;
        riaDocStr = null;
        while(!((sym = fetch()) instanceof Eof)) {
            if(doc == null) {
                doc = riaDocStr;
            }

            if(Objects.equals(sym.kind, ":") && args == null) {
                if(l.size() == 0) {
                    throw new CompileException(sym, "Unexpected `:'");
                }
                XNode colon = (XNode)sym;
                colon.expr = new Node[] {def(null, l, false, null)};
                colon.doc = doc;
                doc = null;
                riaDocStr = null;
                l = new ArrayList<>();
                res.add(sym);
                continue;
            }
            if(Objects.equals(sym.kind, "=")) {
                args = l;
                if(end == '}') {
                    l = new ArrayList<>();
                    l.add(readSeq(' ', "{}"));
                    if((sym = eofWas) instanceof Eof) {
                        break;
                    }
                } else {
                    l = new ArrayList<>();
                    continue;
                }
            }
            if(Objects.equals(sym.kind, ";") || Objects.equals(sym.kind, ",")) {
                if(!Objects.equals(sym.kind, sep)) {
                    break;
                }
                if(args == null && Objects.equals(sep, ";") && l.size() == 0) {
                    continue;
                }
            } else {
                l.add(sym);
                if(!Objects.equals(sep, ";") || !(sym instanceof TypeDef)) {
                    continue; // look for next in line
                }
            }
            if(l.size() == 0) {
                throw new CompileException(sym, "Unexpected " + sym);
            }
            res.add(def(args, l, end == '}', doc));
            if(args != null) {
                doc = null;
            }
            args = null;
            l = new ArrayList<>();
            riaDocStr = null;
        }
        eofWas = sym;
        if(end != ' ' && end != 'e' &&
            (p >= src.length || src[p++] != end)) {
            throw new CompileException(line, p - lineStart + 1, "Expecting " + end);
        }
        if(l.size() != 0) {
            res.add(def(args, l, end == '}', doc));
        } else if(args != null) {
            throw new CompileException(line, p - lineStart, "Expression missing after `='");
        }
        return res.toArray(new Node[0]);
    }

    private Node readSeq(char end, Object kind) {
        String doc = riaDocStr;
        Node[] list = readMany(";", end);
        if(list.length == 1 && kind != Seq.EVAL) {
            if(doc != null && list[0] instanceof Sym) {
                riaDocStr = doc;
            }
            return list[0];
        }
        if(list.length == 0) {
            return new XNode("()", end == ')' ? null : new Node[0])
                .pos(line, p - lineStart);
        }
        // find last element for line/col position
        Node w = list[list.length - 1];
        for(BinOp bo; w instanceof BinOp &&
            (bo = (BinOp)w).left != null; ) {
            w = bo.left;
        }
        Seq rs = new Seq(list, kind);
        rs.checkBind();
        rs.pos(w.line, w.col);
        return rs;
    }

    private Node readSeqTo(String endKind) {
        Node node = readSeq(' ', null);
        if(!Objects.equals(eofWas.kind, endKind)) {
            throw new CompileException(eofWas, "Expected " + endKind + ", found " + eofWas);
        }
        return node;
    }

    private Node readStr() {
        int st = p;
        List<Node> parts = null;
        StringBuilder res = new StringBuilder();
        int sline = line, scol = p - lineStart;
        for(; p < src.length && src[p] != '"'; ++p) {
            if(src[p] == '\n') {
                lineStart = p + 1;
                ++line;
            }
            // Parse embedded lambdas
            if(src[p] == '$' && p + 1 < src.length && src[p + 1] == '{') {
                res.append(src, st, p - st);
                p += 2;
                if(parts == null) {
                    parts = new ArrayList<>();
                }
                if(res.length() != 0) {
                    parts.add(new Str(res.toString()));
                }
                parts.add(readSeq('}', null));
                res.setLength(0);

                st = p;
                --p;
            } else if(src[p] == '\\') {
                res.append(src, st, p - st);
                st = ++p;
                if(p >= src.length) {
                    break;
                }
                switch(src[p]) {
                    case '\\':
                    case '"':
                        continue;
                    case 'a':
                        res.append('\u0007');
                        break;
                    case 'b':
                        res.append('\b');
                        break;
                    case 'f':
                        res.append('\f');
                        break;
                    case 'n':
                        res.append('\n');
                        break;
                    case 'r':
                        res.append('\r');
                        break;
                    case 't':
                        res.append('\t');
                        break;
                    case 'e':
                        res.append('\u001b');
                        break;
                    case '0':
                        res.append('\000');
                        break;
                    case 'u': {
                        st += 4;
                        if(st > src.length) {
                            st = src.length;
                        }
                        int n = st - p;
                        String s = new String(src, p + 1, n);
                        if(n == 4) {
                            try {
                                res.append((char)Integer.parseInt(s, 16));
                                break;
                            } catch(NumberFormatException ignored) {
                            }
                        }
                        throw new CompileException(line, p - lineStart, "Invalid unicode escape code \\u" + s);
                    }
                    default:
                        if(src[p] > ' ') {
                            throw new CompileException(line, p - lineStart, "Unexpected escape: \\" + src[p]);
                        }
                        p = skipSpace();
                        if(p >= src.length || src[p] != '"') {
                            throw new CompileException(line, p - lineStart, "Expecting continuation of string");
                        }
                        st = p;
                }
                ++st;
            }
        }
        if(p >= src.length) {
            throw new CompileException(sline, scol, "Unclosed \"");
        }
        res.append(src, st, p++ - st);
        if(parts == null) {
            return new Str(res.toString());
        }
        if(res.length() != 0) {
            parts.add(new Str(res.toString()));
        }
        return new XNode("concat", parts.toArray(new Node[0]));
    }

    private Str readAStr() {
        int i = p, sline = line, scol = i - lineStart;
        String s = "";
        do {
            for(; i < src.length && src[i] != '\''; ++i) {
                if(src[i] == '\n') {
                    lineStart = i + 1;
                    ++line;
                }
            }
            if(i >= src.length) {
                throw new CompileException(sline, scol, "Unclosed '");
            }
            s = s.concat(new String(src, p, i - p));
            p = ++i;
        } while(i < src.length && src[i++] == '\'');
        return new Str(s);
    }

    String getTypename(Node node) {
        if(!(node instanceof Sym)) {
            throw new CompileException(node, "Expected typename, not a " + node);
        }
        String s = ((Sym)node).sym;
        if(!Character.isLowerCase(s.charAt(0)) && s.charAt(0) != '_') {
            throw new CompileException(node, "Typename must start with lowercase character");
        }
        return s;
    }

    TypeDef readTypeDef() {
        TypeDef def = new TypeDef();
        def.doc = riaDocStr;
        riaDocStr = null;
        def.name = getTypename(fetch());
        List<String> param = new ArrayList<>();
        Node node = fetch();
        if(Objects.equals(def.name, "opaque")) {
            def.kind = TypeDef.OPAQUE;
        } else if(!(node instanceof Sym)) {
        } else if(Objects.equals(def.name, "shared")) {
            def.kind = TypeDef.SHARED;
        } else if(Objects.equals(def.name, "unshare")) {
            def.kind = TypeDef.UNSHARE;
        }
        if(def.kind != 0) {
            def.name = getTypename(node);
            if(def.kind == TypeDef.UNSHARE) {
                def.param = new String[0];
                (def.type = new TypeNode(def.name, new TypeNode[0]))
                    .pos(node.line, node.col);
                return def;
            }
            node = fetch();
        }
        if(node instanceof BinOp && Objects.equals(((BinOp)node).op, "<") &&
            def.kind != TypeDef.SHARED) {
            do {
                param.add(getTypename(fetch()));
            } while(Objects.equals((node = fetch()).kind, ","));
            if(!(node instanceof BinOp) || !Objects.equals(((BinOp)node).op, ">")) {
                throw new CompileException(node, "Expected '>', not a " + node);
            }
            node = fetch();
        }
        if(!Objects.equals(node.kind, "=")) {
            throw new CompileException(node, "Expected '=', not a " + node);
        }
        def.param = param.toArray(new String[0]);
        if((def.type = readType(TYPE_NORMAL)) == null) {
            throw new CompileException(node, "Missing type in typedef declaration");
        }
        return def;
    }

    TypeNode readType(int checkVariant) {
        riaDocStr = null;
        int i = skipSpace();
        if(p >= src.length || src[i] == ')' || src[i] == '>') {
            p = i;
            return null;
        }
        int sline = line, scol = i - lineStart;
        TypeNode res;
        if(src[i] == '(') {
            p = i + 1;
            res = readType(TYPE_NORMAL);
            if(p >= src.length || src[p] != ')') {
                if(res == null) {
                    throw new CompileException(sline, scol, "Unclosed (");
                }
                throw new CompileException(line, p - lineStart, "Expecting ) here");
            }
            ++p;
            if(res == null) {
                res = new TypeNode("()", null);
                res.pos(sline, scol);
            }
        } else if(src[i] == '{') {
            p = i + 1;
            Node t, field;
            ArrayList<TypeNode> param = new ArrayList<>();
            String expect = "Expecting field name or '}' here, not ";
            for(; ; ) {
                riaDocStr = null;
                boolean isVar = Objects.equals((field = fetch()).kind, "var");
                if(Objects.equals(field.kind, "let")) {
                    field = fetch();
                }
                if(isVar) {
                    field = fetch();
                }
                String fieldName;
                if(field instanceof BinOp &&
                    Objects.equals(((BinOp)field).op, FIELD_OP) &&
                    (field = fetch()) instanceof Sym) {
                    fieldName = ".".concat(field.sym());
                } else if(!(field instanceof Sym)) {
                    if(isVar) {
                        throw new CompileException(field, "Expecting field name after var");
                    }
                    break;
                } else {
                    fieldName = field.sym();
                }
                TypeNode f = new TypeNode(fieldName, new TypeNode[1]);
                f.var = isVar;
                f.doc = riaDocStr;
                if(!((t = fetch()) instanceof IsOp) || ((BinOp)t).right != null) {
                    throw new CompileException(t, "Expecting '<:' or 'is' after field name");
                }
                f.param[0] = ((IsOp)t).type;
                param.add(f);
                if(!Objects.equals((field = fetch()).kind, ",")) {
                    expect = "Expecting ',' or '}' here, not ";
                    break;
                }
            }
            if(!Objects.equals(field.kind, "}")) {
                throw new CompileException(field, expect + field);
            }
            ++p;
            res = new TypeNode("",
                param.toArray(new TypeNode[0]));
            res.pos(sline, scol);
        } else {
            do {
                int start = i;
                char c = ' ', dot = '.';
                if(i < src.length && ((c = src[i]) == '~' || c == '^')) {
                    ++i;
                }
                boolean maybeArr = c == '~' || c == '`';
                if(c != '.') {
                    if(Character.isUpperCase(c)) {
                        dot = '_';
                    }
                    while(i < src.length && ((c = src[i]) > '~' ||
                        CHS[c] == 'x' || c == dot || c == '$' || c == '`')) {
                        ++i;
                    }
                    while(src[i - 1] == '.') {
                        --i;
                    }
                }
                if(maybeArr) {
                    c = ' ';
                    while(i + 1 < src.length && // java arrays
                        src[i] == '[' && src[i + 1] == ']') {
                        i += 2;
                    }
                }
                if(i == start) {
                    throw new CompileException(sline, scol,
                        "Expected type identifier, not '" +
                            src[i] + "' in the type expression");
                }
                p = i;
                String sym = new String(src, start, i - start);
                ArrayList<TypeNode> param = new ArrayList<>();
                if(dot == '_') { // Tag variant
                    String doc = riaDocStr;
                    if(i < src.length && src[i] == '.') {
                        ++p;
                    } else {
                        sym = ".".concat(sym);
                    }
                    TypeNode node = readType(TYPE_VARIANT_ARG);
                    if(node == null) {
                        throw new CompileException(line, p - lineStart, "Expecting variant argument");
                    }
                    node = new TypeNode(sym, new TypeNode[] {node});
                    node.doc = doc;
                    node.pos(sline, scol);
                    if(checkVariant == TYPE_VARIANT) {
                        return node;
                    }
                    param.add(node);
                    if(checkVariant != TYPE_VARIANT_ARG) {
                        while((p = skipSpace() + 1) < src.length &&
                            src[p - 1] == '|' &&
                            (node = readType(TYPE_VARIANT)) != null) {
                            param.add(node);
                        }
                        --p;
                    }
                    res = (TypeNode)
                        new TypeNode("|", param.toArray(new TypeNode[0])).pos(sline, scol);
                    break; // break do...while, go check for ->
                }
                if(c == '!') {
                    ++p;
                }
                if((p = skipSpace()) < src.length && src[p] == '<') {
                    ++p;
                    for(TypeNode node; (node = readType(TYPE_NORMAL)) != null;
                        ++p) {
                        param.add(node);
                        if((p = skipSpace()) >= src.length || src[p] != ',') {
                            break;
                        }
                    }
                    if(p >= src.length || src[p] != '>') {
                        throw new CompileException(line, p - lineStart, "Expecting > here");
                    }
                    ++p;
                }
                res = new TypeNode(sym,
                    param.toArray(new TypeNode[0]));
                res.exact = c == '!';
                res.pos(sline, scol);
            } while(false);
        }
        if(checkVariant == TYPE_VARIANT) {
            throw new CompileException(res, "Invalid `| " + res.str() +
                "' in variant type (expecting Tag after `|')");
        }
        p = i = skipSpace();
        if(checkVariant == TYPE_VARIANT_ARG || i + 1 >= src.length ||
            src[i] != '\u2192' && (src[i] != '-' || src[++i] != '>')) {
            return res;
        }
        sline = line;
        scol = p - lineStart;
        p = i + 1;
        TypeNode arg = readType(TYPE_FUNRET);
        if(arg == null) {
            throw new CompileException(sline, scol, "Expecting return type after ->");
        }
        return (TypeNode)new TypeNode("->", new TypeNode[] {res, arg})
            .pos(sline, scol);
    }

    Node parse(Object topLevel) {
        // Skip the first line if it starts with shebang
        if(src.length > 2 && src[0] == '#' && src[1] == '!') {
            for(p = 2; p < src.length && src[p] != '\n'; ++p) {
            }
        }
        int i = p = skipSpace();
        topDoc = riaDocStr;
        while(i < src.length && src[i] < '~' && CHS[src[i]] == 'x') {
            ++i;
        }
        String s = new String(src, p, i - p);
        // We expect the module or program definition first
        // TODO: Should we accept "package" here for compatibility with Java instead of module?
        if(s.equals("module") || s.equals("program")) {
            // Read the name as a series of names separated by dots
            p = i;
            Sym name = readDotted("Expected " + s + " name, not a ");
            moduleName = name.sym;
            moduleNameLine = name.line;
            isModule = s.equals("module");

            if(isModule) {
                // if we are a module then we can be deprecated
                // by adding ":deprecated" to the end of the module name
                if(p < src.length && src[p] == ':') {
                    ++p;
                    Node node = fetch();
                    if(Objects.equals(node.sym(), "deprecated")) {
                        deprecated = true;
                        p = skipSpace();
                    } else if(Objects.equals(node.sym(), "strict")) {
                        // Strict mode is not implemented yet
                    } else {
                        throw new CompileException(node, "Unknown module attribute: " + node);
                    }
                }
            }
            // Finally we expect to see ";" to finish off the module/program statement
            if(p >= src.length || src[p++] != ';') {
                throw new CompileException(line, p - lineStart, "Expected ';' here");
            }
        }
        // TODO: Remove unused variable?
        char first = p < src.length ? src[p] : ' ';
        Node res;
        if((flags & Compiler.CF_EVAL_STORE) != 0) {
            res = readSeq(' ', Seq.EVAL);
            if(res instanceof Seq) {
                Seq seq = (Seq)res;
                Node last = seq.st[seq.st.length - 1];
                if(last instanceof Bind ||
                    Objects.equals(last.kind, "struct-bind") ||
                    Objects.equals(last.kind, "import") ||
                    last instanceof TypeDef) {
                    Node[] tmp = new Node[seq.st.length + 1];
                    System.arraycopy(seq.st, 0, tmp, 0, seq.st.length);
                    tmp[tmp.length - 1] = new XNode("()").pos(seq.line, seq.col);
                    seq.st = tmp;
                    seq.checkBind();
                } else if(seq.st.length == 1) {
                    res = seq.st[0];
                }
            }
        } else {
            res = readSeq(' ', topLevel);
            if(Objects.equals(res.kind, "class")) {
                res = new Seq(new Node[] {res}, topLevel).pos(res.line, res.col);
            }
        }
        if(eofWas != EOF) {
            throw new CompileException(eofWas, "Unexpected " + eofWas);
        }
        return res;
    }
}
