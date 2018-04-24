package ria.lang.compiler.nodes;

import java.util.Objects;

public class TypeNode extends Node {
    public String name;
    public TypeNode[] param;
    public boolean var;
    public boolean exact;
    public String doc;

    public TypeNode(String name, TypeNode[] param) {
        this.name = name;
        this.param = param;
    }

    @Override
    public String str() {
        if(Objects.equals(name, "->")) {
            return "(" + param[0].str() + " -> " + param[1].str() + ")";
        }
        StringBuilder buf = new StringBuilder();
        if(Objects.equals(name, "|")) {
            for(TypeNode aParam : param) {
                buf.append(" | ").append(aParam.str());
            }
            return buf.toString();
        }
        if(Objects.equals(name, "")) {
            buf.append('{');
            for(int i = 0; i < param.length; ++i) {
                if(i != 0) {
                    buf.append("; ");
                }
                buf.append(param[i].name);
                buf.append(" is ");
                buf.append(param[i].param[0].str());
            }
            buf.append('}');
            return buf.toString();
        }
        if(param == null || param.length == 0) {
            return name;
        }
        if(Character.isUpperCase(name.charAt(0))) {
            return "(" + name + " " + param[0].str() + ")";
        }
        buf.append(name);
        buf.append('<');
        for(int i = 0; i < param.length; ++i) {
            if(i != 0) {
                buf.append(", ");
            }
            buf.append(param[i].str());
        }
        buf.append('>');
        return buf.toString();
    }
}
