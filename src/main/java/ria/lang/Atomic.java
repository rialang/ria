package ria.lang;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

class Atomic extends AtomicReference<Object> implements Struct {
    public Atomic(Object value) {
        super(value);
    }

    @Override
    public Object get(String field) {
        if (Objects.equals(field, "value")) {
            return get();
        }
        if (Objects.equals(field, "compareAndSet")) {
            return get(0);
        }
        return get(1); // swap
    }

    @Override
    public Object get(int field) {
        switch (field) {
            case 0:  return new AtomicSet(this, true);  // compareAndSet
            case 1:  return new AtomicSet(this, false); // swap
            default: return get();
        }
    }

    @Override
    public void set(String field, Object value) {
        if (Objects.equals(field, "value")) {
            set(value);
        }
    }

    @Override
    public int count() {
        return 3;
    }

    @Override
    public String name(int field) {
        switch (field) {
            case 0: return "compareAndSet";
            case 1: return "swap";
            default: return "value";
        }
    }

    @Override
    public String eqName(int field) {
        return field == 2 ? "value" : "";
    }

    @Override
    public Object ref(int field, int[] index, int at) {
        index[at + 1] = 0;
        if (field == 2) { // value
            index[at] = field;
            return this;
        }
        index[at] = -1;
        return get(field);
    }
}
