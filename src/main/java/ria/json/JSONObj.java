package ria.json;

import ria.lang.Core;
import ria.lang.Hash;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

// TODO: Rewrite in Ria
@SuppressWarnings("unused")
public class JSONObj {
    private Hash map;

    public JSONObj(Hash map) {
        this.map = map;
    }

    public int hashCode() {
        return map.hashCode();
    }

    public boolean equals(Object o) {
        return o instanceof JSONObj && map.equals(((JSONObj)o).map);
    }

    public Object get(String key) {
        return map.get(key);
    }

    public Set<Object> keySet() {
        return map.keySet();
    }

    public Hash map() {
        return map;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        Iterator<Entry<Object, Object>> i = map.entrySet().iterator();
        for(int n = 0; i.hasNext(); n++) {
            Entry<Object, Object> e = i.next();
            if(n > 0) {
                sb.append(",");
            }
            sb.append(Core.show(e.getKey()));
            sb.append(":");
            Object v = e.getValue();
            sb.append(v == null ? "null" : Core.show(v));
        }
        sb.append("}");
        return sb.toString();
    }
}
