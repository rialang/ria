package ria.lang;

import java.io.Serializable;
import java.util.Objects;

public final class Tag implements Comparable<Tag>, Serializable {
    public final String name;
    public final Object value;

    public Tag(Object aValue, String aName) {
        name = aName;
        value = aValue;
    }

    public int hashCode() {
        return name.hashCode() - (value == null ? 0 : value.hashCode() * 17);
    }

    public boolean equals(Object other) {
        Tag o;
        return other instanceof Tag && Objects.equals(name, (o = (Tag)other).name) &&
               (value == o.value || value != null && value.equals(o.value));
    }

    @Override
    public int compareTo(Tag other) {
        return Objects.equals(name, other.name) ? ((Comparable<Object>) value).compareTo(other.value)
                              : name.compareTo(other.name);
    }

    public String toString() {
        return name + ' ' + Core.show(value);
    }
}
