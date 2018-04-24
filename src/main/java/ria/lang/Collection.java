package ria.lang;

/**
 * Ria core library - a collection.
 */
public interface Collection {
    /**
     * List of collection values.
     */
    AbstractList asList();

    /**
     * Count collection values.
     */
    long length();

    /**
     * Test that collection is empty.
     */
    boolean isEmpty();

    /**
     * Copy collection.
     */
    Object copy();

    /**
     * Remove object by key.
     * This function is member of Collection and not ByKey, because deleteAll
     * can be called on lists, if the keys argument is an empty list.
     */
    void removeAll(AbstractList keys);
}
