package ria.lang;

@SuppressWarnings("unused")
public final class TagConstructor extends Fun {
    private final String name;

    public TagConstructor(String aName) {
        name = aName;
    }

    @Override
    public final Object apply(Object value) {
        return new Tag(value, name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object other) {
        return other instanceof TagConstructor && name.equals(((TagConstructor) other).name);
    }

    public String toString() {
        return name;
    }
}
