package ria.lang;

import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class StringSplit extends Fun {
    private final Pattern p;

    public StringSplit(Object pattern) {
        p = Pattern.compile((String) pattern, Pattern.DOTALL);
    }

    @Override
    public Object apply(Object v) {
        return new MutableList(p.split((CharSequence) v));
    }
}
