package ria.lang;

import java.util.regex.Matcher;

public final class LikeMatcher extends Fun {
    private final Matcher m;

    LikeMatcher(Matcher m) {
        this.m = m;
    }

    @Override
    public Object apply(Object __) {
        if (!m.find()) {
            return new MutableList();
        }
        Object[] r = new Object[m.groupCount() + 1];
        for (int i = r.length; --i >= 0;) {
            String s;
            if ((s = m.group(i)) == null) {
                s = Core.UNDEF_STR;
            }
            r[i] = s;
        }
        return new MutableList(r);
    }
}
