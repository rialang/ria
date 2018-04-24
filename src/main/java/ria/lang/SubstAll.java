package ria.lang;

import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class SubstAll extends Fun2 {
    private final Pattern p;

    public SubstAll(Object pattern) {
        p = Pattern.compile((String) pattern, Pattern.DOTALL);
    }

    @Override
    public Object apply(Object replacement, Object str) {
        return p.matcher((String) str).replaceAll((String) replacement);
    }
}
