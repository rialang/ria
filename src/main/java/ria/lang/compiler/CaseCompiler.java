package ria.lang.compiler;

import ria.lang.compiler.code.CaseExpr;
import ria.lang.compiler.code.Code;
import ria.lang.compiler.nodes.BinOp;
import ria.lang.compiler.nodes.Bind;
import ria.lang.compiler.nodes.Node;
import ria.lang.compiler.nodes.NumLit;
import ria.lang.compiler.nodes.ObjectRefOp;
import ria.lang.compiler.nodes.Str;
import ria.lang.compiler.nodes.Sym;
import ria.lang.compiler.nodes.XNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class CaseCompiler extends RiaType {
    private CaseExpr exp;
    private Scope scope;
    private int depth;
    private List<CType> variants = new ArrayList<>();
    private List<CType> listVars;
    private int submatch; // hack for variants

    private CaseCompiler(Code val, int depth) {
        exp = new CaseExpr(val);
        exp.polymorph = true;
        this.depth = depth;
    }

    private CasePattern toPattern(Node node, CType t, String doc) {
        if((t.flags & RiaType.FL_ANY_PATTERN) != 0) {
            throw new CompileException(node, "Useless case " + node + " (any value already matched)");
        }
        if(t.type == RiaType.VAR && t.ref == null && listVars != null &&
            !listVars.contains(t)) {
            listVars.add(t);
        }
        if(node instanceof Sym) {
            t.flags |= RiaType.FL_ANY_PATTERN;
            String name = node.sym();
            if(Objects.equals(name, "_") || Objects.equals(name, "...")) {
                return CasePattern.ANY_PATTERN;
            }
            BindPattern binding = new BindPattern(exp, t);
            scope = new Scope(scope, name, binding);
            t = t.deref();
            if(t.type == RiaType.VARIANT) {
                t.flags |= RiaType.FL_ANY_PATTERN;
            }
            return binding;
        }
        if(Objects.equals(node.kind, "()")) {
            RiaType.unify(t, RiaType.UNIT_TYPE, node, scope, "#0");
            return CasePattern.ANY_PATTERN;
        }
        if(node instanceof NumLit || node instanceof Str ||
            node instanceof ObjectRefOp) {
            Code c = RiaAnalyzer.analyze(node, scope, depth);
            if(!(node instanceof ObjectRefOp) || c.flagop(Code.CONST)) {
                t = t.deref();
                if(t.type == RiaType.VAR) {
                    t.type = c.type.type;
                    t.param = RiaType.NO_PARAM;
                    t.flags = RiaType.FL_PARTIAL_PATTERN;
                } else if(t.type != c.type.type) {
                    throw new CompileException(node, scope, c.type, t, "Pattern type mismatch: #~", null);
                }
                return new ConstPattern(c);
            }
        }
        if(Objects.equals(node.kind, "list")) {
            //noinspection ConstantConditions - if we have a list node, it must be XNode
            XNode list = (XNode)node;
            CType itemt = new CType(depth);
            CType lt = new CType(RiaType.MAP,
                new CType[] {itemt, new CType(depth), RiaType.LIST_TYPE});
            lt.flags |= RiaType.FL_PARTIAL_PATTERN;
            if(list.expr == null || list.expr.length == 0) {
                RiaType.unify(t, lt, node, scope, "#0");
                return AbstractListPattern.EMPTY_PATTERN;
            }
            CasePattern[] items = new CasePattern[list.expr.length];
            int anyitem = RiaType.FL_ANY_PATTERN;
            ++submatch;
            List<CType> oldListVars = listVars;
            listVars = new ArrayList<>();
            for(int i = 0; i < items.length; ++i) {
                itemt.flags &= ~RiaType.FL_ANY_PATTERN;
                for(int j = listVars.size(); --j >= 0; ) {
                    (listVars.get(j)).flags &= ~RiaType.FL_ANY_PATTERN;
                }
                listVars.clear();
                items[i] = toPattern(list.expr[i], itemt, null);
                anyitem &= itemt.flags;
            }
            listVars = oldListVars;
            --submatch;
            itemt.flags &= anyitem;
            RiaType.unify(t, lt, node, scope, "#0");
            return new ListPattern(items);
        }
        if(node instanceof BinOp) {
            BinOp pat = (BinOp)node;
            if(Objects.equals(pat.op, "") && pat.left instanceof Sym) {
                String variant = pat.left.sym();
                if(!Character.isUpperCase(variant.charAt(0))) {
                    throw new CompileException(pat.left, variant + ": Variant constructor must start with upper case");
                }
                t = t.deref();
                if(t.type != RiaType.VAR && t.type != RiaType.VARIANT) {
                    throw new CompileException(node, "Variant " + variant + " ... is not " + t.toString(scope, null));
                }
                t.type = RiaType.VARIANT;
                if(t.requiredMembers == null) {
                    t.requiredMembers = new HashMap<>();
                    t.flags |= RiaType.FL_ANY_CASE;
                    if(submatch == 0) // XXX hack!!!
                    {
                        variants.add(t);
                    }
                }
                CType argt = new CType(depth);
                argt.doc = doc;
                CType old = t.requiredMembers.put(variant, argt);
                if(old != null) {
                    argt = RiaType.withDoc(old, doc);
                    t.requiredMembers.put(variant, argt);
                }
                CasePattern arg = toPattern(pat.right, argt, null);
                RiaType.structParam(t, t.requiredMembers, new CType(depth));
                return new VariantPattern(variant, arg);
            }
            if(Objects.equals(pat.op, ":+")) {
                CType itemt = new CType(depth);
                // It must must have the NO_TYPE constraint,
                // because tail has the same type as the matched
                // (this could be probably solved by giving tail
                //  and pattern separate list types, but then
                //  correct use of pattern flags must be considered)
                CType lt = new CType(RiaType.MAP,
                    new CType[] {itemt, RiaType.NO_TYPE, RiaType.LIST_TYPE});
                int flags = t.flags;
                RiaType.unify(t, lt, node, scope, "#0");
                ++submatch;
                CasePattern hd = toPattern(pat.left, itemt, null);
                CasePattern tl = toPattern(pat.right, t, null);
                --submatch;
                lt.flags = RiaType.FL_PARTIAL_PATTERN;
                t.flags = flags;
                return new ConsPattern(hd, tl);
            }
        }
        if(Objects.equals(node.kind, "struct")) {
            //noinspection ConstantConditions - if we have a struct, then it must be an XNode
            Node[] fields = ((XNode)node).expr;
            if(fields.length == 0) {
                throw new CompileException(node, RiaAnalyzer.EMPTY_STRUCT);
            }
            String[] names = new String[fields.length];
            CasePattern[] patterns = new CasePattern[fields.length];
            HashSet<String> uniq = new HashSet<>(fields.length);
            int allAny = RiaType.FL_ANY_PATTERN;

            for(int i = 0; i < fields.length; ++i) {
                Bind field = RiaAnalyzer.getField(fields[i]);
                if(uniq.contains(field.name)) {
                    RiaAnalyzer.duplicateField(field);
                }
                uniq.add(field.name);
                CType ft = new CType(depth);
                CType part = new CType(RiaType.STRUCT,
                    new CType[] {new CType(depth), ft});
                HashMap<String, CType> tm = new HashMap<>();
                tm.put(field.name, ft);
                part.requiredMembers = tm;
                RiaType.unify(t, part, field, scope, "#0");
                names[i] = field.name;
                ft.flags &= ~RiaType.FL_ANY_PATTERN;
                patterns[i] = toPattern(field.expr, ft, null);
                allAny &= ft.flags;
            }

            Map<String, CType> tm = t.deref().requiredMembers;

            if(tm != null) {
                for(CType cType : tm.values()) {
                    CType ft = cType.deref();
                    if(allAny == 0) {
                        ft.flags &= ~RiaType.FL_ANY_PATTERN;
                    } else {
                        ft.flags |= RiaType.FL_ANY_PATTERN;
                    }
                }
            }
            return new StructPattern(names, patterns);
        }
        throw new CompileException(node, "Bad case pattern: " + node);
    }

    private void finalizeVariants() {
        for(int i = variants.size(); --i >= 0; ) {
            CType t = variants.get(i);
            if(t.type == RiaType.VARIANT && t.allowedMembers == null &&
                (t.flags & RiaType.FL_ANY_PATTERN) == 0) {
                t.allowedMembers = t.requiredMembers;
                t.requiredMembers = null;
                t.flags &= ~RiaType.FL_ANY_CASE;
            }
        }
    }

    private void mergeChoice(CasePattern pat, Node node, Scope scope) {
        Code opt = RiaAnalyzer.analyze(node, scope, depth);
        exp.polymorph &= opt.polymorph;
        if(exp.type == null) {
            exp.type = opt.type;
        } else {
            try {
                exp.type = RiaType.mergeOrUnify(exp.type, opt.type);
            } catch(TypeException e) {
                throw new CompileException(node, scope, opt.type, exp.type, "This choice has a #1 type, while another was a #2", e);
            }
        }
        exp.addChoice(pat, opt);
    }

    private static String checkPartialMatch(CType t) {
        if(t.seen || (t.flags & RiaType.FL_ANY_PATTERN) != 0) {
            return null;
        }
        if((t.flags & RiaType.FL_PARTIAL_PATTERN) != 0) {
            return t.type == RiaType.MAP ? "[]" : t.toString();
        }
        if(t.type != RiaType.VAR) {
            t.seen = true;
            for(int i = t.param.length; --i >= 0; ) {
                String s = checkPartialMatch(t.param[i]);
                if(s != null) {
                    t.seen = false;
                    if(t.type == RiaType.MAP) {
                        return "(" + s + ")::_";
                    }
                    if(t.type == RiaType.VARIANT || t.type == RiaType.STRUCT) {
                        for(Map.Entry<String, CType> e : t.requiredMembers.entrySet()) {
                            if(e.getValue() == t.param[i]) {
                                return (t.type == RiaType.STRUCT ? "." : "") + e.getKey() + " (" + s + ")";
                            }
                        }
                    }
                    return s;
                }
            }
            t.seen = false;
        } else if(t.ref != null) {
            return checkPartialMatch(t.ref);
        }
        return null;
    }

    static Code caseType(XNode ex, Scope scope, int depth) {
        Node[] choices = ex.expr;
        if(choices.length <= 1) {
            throw new CompileException(ex, "case expects some option!");
        }
        Code val = RiaAnalyzer.analyze(choices[0], scope, depth);
        CaseCompiler cc = new CaseCompiler(val, depth);
        CasePattern[] pats = new CasePattern[choices.length];
        Scope[] scopes = new Scope[choices.length];
        CType argType = new CType(depth);
        for(int i = 1; i < choices.length; ++i) {
            cc.scope = scope;
            XNode choice = (XNode)choices[i];
            pats[i] = cc.toPattern(choice.expr[0], argType, choice.doc);
            scopes[i] = cc.scope;
            cc.exp.resetParams();
        }
        String partialError = checkPartialMatch(argType);
        if(partialError != null) {
            throw new CompileException(ex, "Partial match: " + partialError);
        }
        cc.finalizeVariants();
        for(int i = 1; i < choices.length; ++i) {
            if(!Objects.equals(choices[i].kind, "...")) {
                cc.mergeChoice(pats[i], ((XNode)choices[i]).expr[1], scopes[i]);
            }
        }
        RiaType.unify(val.type, argType, choices[0], scope,
            "Inferred type for case argument is #2, but a #1 is given\n    (#0)");
        return cc.exp;
    }
}
