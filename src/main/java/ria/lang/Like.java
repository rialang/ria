package ria.lang;

import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class Like extends Fun {
    private Pattern p;

    public Like(Object pattern) {
        p = Pattern.compile((String) pattern, Pattern.DOTALL);
    }

    @Override
    public Object apply(Object v) {
        return new LikeMatcher(p.matcher((CharSequence) v));
    }

    public String toString() {
        return "<like " + Core.show(p.pattern()) + ">";
    }
}
