package ria.lang;

import java.util.Objects;

public class Struct3 extends AbstractStruct {
    public Object _0;
    public Object _1;
    public Object _2;

    public Struct3(String[] names, boolean[] vars) {
        super(names, vars);
    }

    @Override
    public Object get(String field) {
        String[] a = names;
        if(Objects.equals(a[0], field)) {
            return _0;
        }
        if(Objects.equals(a[1], field)) {
            return _1;
        }
        if(Objects.equals(a[2], field)) {
            return _2;
        }
        return null;
    }

    @Override
    public Object get(int field) {
        switch(field) {
            case 0:
                return _0;
            case 1:
                return _1;
            case 2:
                return _2;
        }
        return null;
    }

    @Override
    public void set(String field, Object value) {
        String[] a = names;
        if(Objects.equals(a[0], field)) {
            _0 = value;
        } else if(Objects.equals(a[1], field)) {
            _1 = value;
        } else if(Objects.equals(a[2], field)) {
            _2 = value;
        }
    }
}
