package ria.lang;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

/**
 * Ria core library - IdentityHash.
 */
public class CHash extends AbstractMap implements ByKey, Collection {
    @SuppressWarnings("WeakerAccess")
    public  static final int CUSTOM = 0;
    @SuppressWarnings("WeakerAccess")
    public static final int IDENTITY = 1;
    @SuppressWarnings("WeakerAccess")
    public static final int CONCURRENT = 2;
    @SuppressWarnings("WeakerAccess")
    public static final int WEAK = 3;
    private final int type;
    private final Fun cons;
    private final Map<Object, Object> impl;
    private volatile Fun defaultFun;

    public CHash(int type_, Fun cons_) {
        type = type_;
        cons = cons_;
        switch(type) {
            case CUSTOM:
                impl = (Map<Object, Object>)cons_.apply(null);
                break;
            case IDENTITY:
                impl = new java.util.IdentityHashMap<>();
                break;
            case CONCURRENT:
                impl = new java.util.concurrent.ConcurrentHashMap<>();
                break;
            case WEAK:
                impl = new java.util.WeakHashMap<>();
                break;
            default:
                throw new IllegalArgumentException("Invalid CHash type " + type_);
        }
    }

    @Override
    public void clear() {
        impl.clear();
    }

    @Override
    public Set keySet() {
        return impl.keySet();
    }

    @Override
    public Set entrySet() {
        return impl.entrySet();
    }

    @Override
    public boolean isEmpty() {
        return impl.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return impl.containsKey(key);
    }

    @Override
    public Object get(Object key) {
        return impl.get(key);
    }

    @Override
    public Object put(Object key, Object value) {
        return impl.put(key, value);
    }

    @Override
    public void putAll(Map m) {
        impl.putAll(m);
    }

    @Override
    public Object remove(Object key) {
        return impl.remove(key);
    }

    public int hashCode() {
        return impl.hashCode();
    }

    public boolean equals(Object o) {
        return impl.equals(o);
    }

    @Override
    public Object vget(Object key) {
        Object x;
        if((x = impl.get(key)) == null && !impl.containsKey(key)) {
            if(defaultFun != null) {
                return defaultFun.apply(key);
            }
            throw new NoSuchKeyException("Key not found (" + key + ")");
        }
        return x;
    }

    @Override
    public void removeAll(AbstractList keys) {
        if(keys != null && !keys.isEmpty()) {
            for(AbstractIterator i = keys; i != null; i = i.next()) {
                impl.remove(i.first());
            }
        }
    }

    @Override
    public int size() {
        return impl.size();
    }

    @Override
    public long length() {
        return impl.size();
    }

    @Override
    public AbstractList asList() {
        return new MutableList(impl.values().toArray());
    }

    @Override
    public void setDefault(Fun fun) {
        defaultFun = fun;
    }

    @Override
    public Object copy() {
        CHash result = new CHash(type, cons);
        result.putAll(this);
        result.defaultFun = defaultFun;
        return result;
    }
}
