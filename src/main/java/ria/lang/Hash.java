package ria.lang;

import java.util.Iterator;
import java.util.Map;

/** Ria core library - Hash. */
public class Hash extends java.util.HashMap<Object, Object> implements ByKey, Collection {
    private Fun defaultFun;

    public Hash() {
    }

    public Hash(int initialCapacity) {
        super(initialCapacity);
    }

    public Hash(Map<Object, Object> map) {
        super(map);
    }

    @Override
    public Object vget(Object key) {
        Object x;
        if ((x = get(key)) == null && !containsKey(key)) {
            if (defaultFun != null) {
                return defaultFun.apply(key);
            }
            throw new NoSuchKeyException("Key not found (" + key + ")");
        }
        return x;
    }

    @Override
    public void removeAll(AbstractList keys) {
        if (keys != null && !keys.isEmpty()) {
            for (AbstractIterator i = keys; i != null; i = i.next()) {
                remove(i.first());
            }
        }
    }

    @Override
    public long length() {
        return size();
    }

    @Override
    public AbstractList asList() {
        return new MutableList(values().toArray());
    }

    @Override
    public void setDefault(Fun fun) {
        defaultFun = fun;
    }
    
    @Override
    public Object copy() {
        Hash result = new Hash(this);
        result.defaultFun = defaultFun;
        return result;
    }

    public String toString() {
        int size = size();
        if (size == 0) {
            return "[:]";
        }

        StringBuilder sb = new StringBuilder("[");
        Iterator<Entry<Object, Object>> i = entrySet().iterator();
        for(int n = 0; i.hasNext(); n++) {
            Entry<Object, Object> e = i.next();
            if(n > 0) {
                sb.append(",");
            }
            sb.append(Core.show(e.getKey()));
            sb.append(":");
            Object v = e.getValue();
            sb.append(Core.show(v));
        }
        sb.append("]");
        return sb.toString();
    }
}
