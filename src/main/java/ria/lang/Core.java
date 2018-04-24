package ria.lang;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;

public final class Core {
    public static final String UNDEF_STR = "";
    public static final ThreadLocal<MutableList> ARGV = ThreadLocal.withInitial(MutableList::new);
    private static final int DEC_SHIFT[] = {1, 10, 100, 1000, 10000,
        100000, 1000000, 10000000, 100000000, 1000000000};
    private static final char base64[] =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
            .toCharArray();

    public static String replace(String f, String r, String s) {
        StringBuilder result = new StringBuilder();
        int p = 0, i, l = f.length();
        while((i = s.indexOf(f, p)) >= 0) {
            result.append(s, p, i);
            result.append(r);
            p = i + l;
        }
        if(p < s.length()) {
            result.append(s.substring(p));
        }
        return result.toString();
    }

    public static RiaNum parseNum(String str) {
        String s = str.trim();
        int l;
        if((l = s.length()) == 0) {
            throw new IllegalArgumentException("Number expected");
        }
        int radix = 10, st = s.charAt(0) == '-' ? 1 : 0;
        if(l > 2 && s.charAt(st) == '0') {
            // Fallthrough should probably be rewritten
            switch(s.charAt(st + 1)) {
                case 'o':
                case 'O':
                    radix = 2;
                case 'x':
                case 'X':
                    s = s.substring(st += 2);
                    if(st != 2) {
                        s = "-".concat(s);
                    }
                    radix += 6;
            }
        }
        if(radix == 10) {
            if(s.indexOf('e') >= 0 || s.indexOf('E') >= 0) {
                char c;
                if((c = s.charAt(l - 1)) == 'e' || c == 'E') {
                    return new FloatNum(Double.parseDouble(s.substring(0, l - 1)));
                }
                return new FloatNum(Double.parseDouble(s));
            }
            int dot = s.indexOf('.');
            if(dot == l - 1) {
                s = s.substring(0, dot);
                dot = -1;
            }

            if(dot > 0) {
                while(s.charAt(--l) == '0') {
                }
                if(s.charAt(l) != '.') {
                    if(l <= 11) {
                        long n = Long.parseLong(s.substring(0, dot).concat(s.substring(dot + 1, l + 1)));
                        if(n >= Integer.MIN_VALUE && n <= Integer.MAX_VALUE) {
                            return new RatNum((int)n, DEC_SHIFT[l - dot]);
                        }
                    }
                    return new FloatNum(Double.parseDouble(s));
                } else {
                    s = s.substring(0, l);
                }
            }
        }

        if((l - st) < 96 / radix + 10) // 22, 19, 16
        {
            return new IntNum(Long.parseLong(s, radix));
        }
        return new BigNum(s, radix);
    }

    public static String concat(String[] param) {
        StringBuilder sb = new StringBuilder();
        for(String s : param) {
            sb.append(s);
        }

        return sb.toString();
    }

    public static String show(Object o) {
        StringBuffer r;
        if(o == null) {
            return "[]";
        }
        if(o instanceof String) {
            // TODO: proper escaping
            // TODO: This code is awful, needs to be rewritten
            char[] s = ((String)o).toCharArray();
            r = new StringBuffer().append('"');
            int p = 0, i = 0, cnt = s.length;
            for(String c; i < cnt; ++i) {
                if(s[i] == '\\') {
                    c = "\\\\";
                } else if(s[i] == '"') {
                    c = "\\\"";
                } else if(s[i] == '\n') {
                    c = "\\n";
                } else if(s[i] == '\r') {
                    c = "\\r";
                } else if(s[i] == '\t') {
                    c = "\\t";
                } else if(s[i] >= '\u0000' && s[i] < ' ') {
                    c = "000".concat(Integer.toHexString(s[i]));
                    c = "\\u".concat(c.substring(c.length() - 4));
                } else {
                    continue;
                }
                r.append(s, p, i - p).append(c);
                p = i + 1;
            }
            return r.append(s, p, i - p).append('"').toString();
        }
        if(o.getClass().isArray()) {
            r = new StringBuffer().append('[');
            for(int i = 0, len = Array.getLength(o); i < len; ++i) {
                if(i != 0) {
                    r.append(',');
                }
                if(i == 50 && len > 110) {
                    r.append("...");
                    i = len - 50;
                }
                r.append(Array.get(o, i));
            }
            return r.append(']').toString();
        }
        return o.toString();
    }

    @SuppressWarnings("unused")
    static String read(java.io.Reader r, int max) throws IOException {
        char[] buf = new char[max];
        int n = r.read(buf, 0, max);
        return n < 0 ? null : new String(buf, 0, n);
    }

    @SuppressWarnings("unused")
    static String readAll(java.io.Reader r) throws IOException {
        StringBuilder result = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while((n = r.read(buf, 0, buf.length)) > 0) {
            result.append(buf, 0, n);
        }
        return result.toString();
    }

    static AbstractList readAll(int limit, Fun read, Fun close) {
        byte[] buf = new byte[0 < limit && limit <= 65536 ? limit : 8192];
        int l = 0, n;
        try {
            while((n = ((Number)read.apply(buf, new IntNum(l)))
                .intValue()) >= 0) {
                if(buf.length - (l += n) < 2048) {
                    int reserve = buf.length << 1;
                    if(limit > 0 && reserve > limit) {
                        if(buf.length >= limit) {
                            //noinspection ThrowableNotThrown - We do actually throw this
                            Unsafe.unsafeThrow(new IOException("Read limit " + limit + " exceeded"));
                        }
                        reserve = limit;
                    }
                    byte[] tmp = new byte[reserve];
                    System.arraycopy(buf, 0, tmp, 0, l);
                    buf = tmp;
                }
            }
        } finally {
            close.apply(null);
        }
        return l > 0 ? new ByteArray(0, l, buf) : null;
    }

    @SuppressWarnings("unused")
    public static void setArgv(String[] argv) {
        if(argv != null) {
            ARGV.set(new MutableList(argv));
        }
    }

    @SuppressWarnings("unused")
    public static Object badMatch(Object match) {
        throw new BadMatch(match, null, 0, 0);
    }

    @SuppressWarnings("unused")
    public static String capitalize(String s) {
        char[] tmp = s.toCharArray();
        if(tmp.length == 0) {
            return s;
        }
        tmp[0] = Character.toUpperCase(tmp[0]);
        return new String(tmp);
    }

    @SuppressWarnings("unused")
    static String uncapitalize(String s) {
        char[] tmp = s.toCharArray();
        if(tmp.length == 0) {
            return s;
        }
        tmp[0] = Character.toLowerCase(tmp[0]);
        return new String(tmp);
    }

    @SuppressWarnings("unused")
    static String b64enc(byte[] buf, int len) {
        char[] res = new char[(len + 2) / 3 * 4];
        for(int s = 0, d = 0; len > 0; len -= 3) {
            res[d] = base64[buf[s] >>> 2 & 63];
            res[d + 1] = base64[((buf[s] & 3) << 4) |
                (len > 1 ? buf[s + 1] >>> 4 & 15 : 0)];
            res[d + 2] = len > 1 ? base64[((buf[s + 1] & 15) << 2) |
                (len > 2 ? buf[s + 2] >>> 6 & 3 : 0)] : '=';
            res[d + 3] = len > 2 ? base64[buf[s + 2] & 63] : '=';
            s += 3;
            d += 4;
        }
        return new String(res);
    }

    @SuppressWarnings("unused")
    static AbstractList b64dec(String src) {
        int n = 0, outp = 0;
        byte[] buf = new byte[src.length() * 3 / 4];

        for(int s = 0, len = src.length(); s < len; ++s) {
            char c = src.charAt(s);
            int v = c == '+' ? 0x3e : c == '/' ? 0x3f : c >= 'A' && c <= 'Z'
                ? c - 'A' : c >= 'a' && c <= 'z'
                ? c - 'G' : c >= '0' && c <= '9' ? c + 4 : -1;
            if(v == -1) {
                if(c == '=') {
                    break;
                }
                continue;
            }
            switch(n) {
                case 0:
                    buf[outp] = (byte)(v << 2);
                    break;
                case 1:
                    buf[outp] |= v >>> 4;
                    buf[outp + 1] = (byte)((v & 15) << 4);
                    break;
                case 2:
                    buf[outp + 1] |= v >>> 2;
                    buf[outp + 2] = (byte)((v & 3) << 6);
                    break;
                case 3:
                    buf[outp + 2] |= v;
                    outp += 3;
                    n = -1;
                    break;
            }
            ++n;
        }
        if(n > 0) // 1, 2, 3
        {
            outp += n - 1;
        }
        return outp > 0 ? new ByteArray(0, outp, buf) : null;
    }

    @SuppressWarnings("unused")
    public static byte[] bytes(AbstractList list) {
        if(list == null) {
            return new byte[0];
        }
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            AbstractIterator i = list;
            while(i != null) {
                i = i.write(buf);
            }
            return buf.toByteArray();
        } catch(IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
