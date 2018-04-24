package ria.lang;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MatchAllFun extends Fun {
    final Pattern pattern;
    final Fun matchFun;
    final Fun skipFun;

    final class Match extends RiaList {
        private AbstractList rest;
        private final int last;
        private final String str;
        private Matcher m;

        Match(Object v, int last_, String str_, Matcher m_) {
            super(v, null);
            last = last_;
            str = str_;
            m = m_;
        }

        @Override
        public synchronized AbstractList rest() {
            if (m != null) {
                rest = get(str, m, last);
                m = null;
            }
            return rest;
        }
    }

    MatchAllFun(Pattern pattern_, Fun matchFun_, Fun skipFun_) {
        pattern = pattern_;
        matchFun = matchFun_;
        skipFun = skipFun_;
    }

    AbstractList get(String s, Matcher m, int last) {
        if (!m.find()) {
            return (s = s.substring(last)).length() == 0
                    ? null : new RiaList(skipFun.apply(s), null);
        }
        int st = m.start();
        Object skip = last >= st ? null :
            skipFun.apply(s.substring(last, st));
        Object[] r = new Object[m.groupCount() + 1];
        for (int i = r.length; --i >= 0;) {
            String g;
            if ((g = m.group(i)) == null) {
                g = Core.UNDEF_STR;
            }
            r[i] = g;
        }
        Match l = new Match(matchFun.apply(new MutableList(r)), m.end(), s, m);
        return last < st ? new RiaList(skip, l) : l;
    }

    @Override
    public Object apply(Object str) {
        String s = (String) str;
        return get(s, pattern.matcher(s), 0);
    }
}
