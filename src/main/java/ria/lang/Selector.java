package ria.lang;

public final class Selector extends Fun {
    private final String name;

    public Selector(String aName) {
        name = aName;
    }

    @Override
    public final Object apply(Object value) {
        return ((Struct)value).get(name);
    }

    public String toString() {
        return "(." + name + ')';
    }
}
