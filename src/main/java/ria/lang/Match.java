package ria.lang;

import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class Match extends Fun {
    private final Pattern p;
    private final boolean yes;

    public Match(Object pattern, boolean yes) {
        p = Pattern.compile((String) pattern, Pattern.DOTALL);
        this.yes = yes;
    }

    @Override
    public Object apply(Object v) {
        return p.matcher((CharSequence) v).find() == yes ? Boolean.TRUE : Boolean.FALSE;
    }
}
