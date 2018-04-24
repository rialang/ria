package ria.lang;

/**
 * Ria core library - ByKey.
 */
public interface ByKey {
    /**
     * Get object by key. Throw an Exception on error.
     */
    Object vget(Object key);

    /**
     * Put object by key.
     */
    Object put(Object key, Object value);

    /**
     * Remove object by key.
     */
    Object remove(Object key);

    /**
     * Has a given key.
     */
    boolean containsKey(Object key);

    /**
     * Set a default function.
     */
    void setDefault(ria.lang.Fun fun);
}
