package ria.lang;

import java.io.Serializable;

public abstract class Fun implements Serializable {
    public abstract Object apply(Object arg);

    public Object apply(Object a, Object b) {
        return ((Fun) apply(a)).apply(b);
    }

    public String toString() {
        return '<' + getClass().getName() + '>';
    }
}
