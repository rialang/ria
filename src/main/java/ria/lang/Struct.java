package ria.lang;

public interface Struct {
    /**
     * Get field by interned name.
     * Warning: the behaviour is undefined when field does not exists!
     */
    Object get(String field);

    /**
     * Get field by index (corresponding to name(field)).
     * Warning: the behaviour is undefined when field does not exists!
     */
    Object get(int field);

    /**
     * Set field by interned name to given value.
     * Warning: the behaviour is undefined when field does not exists!
     */
    void set(String field, Object value);

    /**
     * Field count.
     */
    int count();

    /**
     * Field name by field index (must be sorted alphabetically).
     * Warning: the behaviour is undefined when field does not exists!
     */
    String name(int field);

    /**
     * Field name by field index for equality operations.
     * If the field should not participate in equality comparisions,
     * then eqName should return the empty string literal "" instance
     * (so that the returned value == "" is true).
     */
    String eqName(int field);

    /**
     * Returns reference struct or field value.
     * If the field is immutable, then the field value will be returned
     * and index[at] is assigned -1. Otherwise a reference struct is
     * returned and index[at] is assigned a field index in the returned struct.
     * The index[at + 1] is assigned 1 when the field shouldn't participate
     * in equality comparisions (eqName(field) == ""), and 0 otherwise.
     */
    Object ref(int field, int[] index, int at);
}
