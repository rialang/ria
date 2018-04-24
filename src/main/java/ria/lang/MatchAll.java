package ria.lang;

import java.util.regex.Pattern;

final public class MatchAll extends Fun2 {
    private final Pattern p;

    public MatchAll(Object pattern) {
        p = Pattern.compile((String) pattern, Pattern.DOTALL);
    }

    @Override
    public Object apply(Object matchFun, Object skipFun) {
        return new MatchAllFun(p, (Fun) matchFun, (Fun) skipFun);
    }
}
