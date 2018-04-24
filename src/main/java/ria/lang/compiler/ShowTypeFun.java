package ria.lang.compiler;

import ria.lang.AbstractIterator;
import ria.lang.AbstractList;
import ria.lang.Core;
import ria.lang.Fun;
import ria.lang.Fun2;
import ria.lang.Struct;
import ria.lang.Tag;

import java.util.Objects;

public class ShowTypeFun extends Fun2 {
    Fun showType;
    Fun formatDoc;
    String indentStep = "   ";

    ShowTypeFun() {
        showType = this;
    }

    private void hstr(StringBuffer to, boolean variant,
                      AbstractList fields, String indent) {
        boolean useNL = false;
        AbstractIterator i = fields;
        for(int n = 0; i != null; i = i.next()) {
            if(++n >= 3 || formatDoc != null && ((String)((Struct)
                i.first()).get("description")).length() > 0) {
                useNL = true;
                break;
            }
        }

        String indent_ = indent, oldIndent = indent;
        if(useNL) {
            if(!variant) {
                indent = indent.concat(indentStep);
            }
            indent_ = indent.concat(indentStep);
        }

        String sep = variant
            ? useNL ? "\n" + indent + "| " : " | "
            : useNL ? ",\n".concat(indent) : ", ";

        Struct field = null;
        for(i = fields; i != null; i = i.next()) {
            field = (Struct)i.first();
            if(i != fields) // not first
            {
                to.append(sep);
            } else if(useNL && !variant) {
                to.append('\n').append(indent);
            }
            if(formatDoc != null) {
                String doc = (String)field.get("description");
                if(formatDoc != this) {
                    to.append(formatDoc.apply(indent, doc));
                } else if(useNL && doc.length() > 0) {
                    to.append("// ")
                        .append(Core.replace("\n", "\n" + indent + "//", doc))
                        .append('\n')
                        .append(indent);
                }
            }
            if(!variant) {
                to.append(field.get("mutable") == Boolean.TRUE ? "var " : "let ");
                to.append(field.get("tag"));
            }
            to.append(field.get("name"));
            if(variant) {
                to.append(field.get("tag"));
            }
            to.append(variant ? " " : " is ");
            Tag fieldType = (Tag)field.get("type");
            Object tstr = showType.apply(indent_, fieldType);
            if(variant && (Objects.equals(fieldType.name, "Function") ||
                Objects.equals(fieldType.name, "Variant"))) {
                to.append('(').append(tstr).append(')');
            } else {
                to.append(tstr);
            }
        }
        try {
            if(field != null && field.get("strip") != null) {
                to.append(sep).append("...");
            }
        } catch(Exception ignored) {
        }
        if(useNL && !variant) {
            to.append("\n").append(oldIndent);
        }
    }

    @Override
    public Object apply(Object indent, Object typeObj) {
        Tag type = (Tag)typeObj;
        String typeTag = type.name;
        if(Objects.equals(typeTag, "Simple")) {
            return type.value;
        }
        if(Objects.equals(typeTag, "Alias")) {
            Struct t = (Struct)type.value;
            return '(' + (String)t.get("alias") + " is " +
                showType.apply(indent, t.get("type")) + ')';
        }

        AbstractList typeList;
        String typeName = null;
        if(Objects.equals(typeTag, "Parametric")) {
            Struct t = (Struct)type.value;
            typeName = (String)t.get("type");
            typeList = (AbstractList)t.get("params");
        } else {
            typeList = (AbstractList)type.value;
        }
        if(typeList != null && typeList.isEmpty()) {
            typeList = null;
        }
        AbstractIterator i = typeList;
        StringBuffer to = new StringBuffer();

        if(typeName != null) {
            to.append(typeName).append('<');
            for(; i != null; i = i.next()) {
                if(i != typeList) {
                    to.append(", ");
                }
                to.append(showType.apply(indent, i.first()));
            }
            to.append('>');
        } else if(Objects.equals(typeTag, "Function")) {
            while(i != null) {
                Tag t = (Tag)i.first();
                if(i != typeList) {
                    to.append(" -> ");
                }
                i = i.next();
                if(i != null && Objects.equals(t.name, "Function")) {
                    to.append('(')
                        .append(showType.apply(indent, t))
                        .append(')');
                } else {
                    to.append(showType.apply(indent, t));
                }
            }
        } else if(Objects.equals(typeTag, "Struct")) {
            to.append('{');
            hstr(to, false, typeList, (String)indent);
            to.append('}');
        } else if(Objects.equals(typeTag, "Variant")) {
            hstr(to, true, typeList, (String)indent);
        } else {
            throw new IllegalArgumentException("Unknown type kind: " + typeTag);
        }
        return to.toString();
    }
}
