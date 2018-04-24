package ria.lang;

import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class RiaNum extends Number implements Comparable<Object> {
    private static final long[] SHL_LIMIT = {
        Long.MAX_VALUE, 0x4000000000000000L,
        0x2000000000000000L, 0x1000000000000000L,
        0x0800000000000000L, 0x0400000000000000L,
        0x0200000000000000L, 0x0100000000000000L,
        0x0080000000000000L, 0x0040000000000000L,
        0x0020000000000000L, 0x0010000000000000L,
        0x0008000000000000L, 0x0004000000000000L,
        0x0002000000000000L, 0x0001000000000000L,
        0x800000000000L, 0x400000000000L, 0x200000000000L, 0x100000000000L,
        0x080000000000L, 0x040000000000L, 0x020000000000L, 0x010000000000L,
        0x008000000000L, 0x004000000000L, 0x002000000000L, 0x001000000000L,
        0x000800000000L, 0x000400000000L, 0x000200000000L, 0x000100000000L,
        0x000080000000L, 0x000040000000L, 0x000020000000L, 0x000010000000L,
        0x000008000000L, 0x000004000000L, 0x000002000000L, 0x000001000000L,
        0x000000800000L, 0x000000400000L, 0x000000200000L, 0x000000100000L,
        0x000000080000L, 0x000000040000L, 0x000000020000L, 0x000000010000L,
        0x8000L, 0x4000L, 0x2000L, 0x1000L, 0x0800L, 0x0400L, 0x0200L, 0x0100L,
        0x0080L, 0x0040L, 0x0020L, 0x0010L, 0x0008L, 0x0004L, 0x0002L, 0x0001L,
    };

    public static RiaNum parseNum(String str) {
        return Core.parseNum(str);
    }

    public abstract RiaNum add(RiaNum num);

    public abstract RiaNum add(long num);

    public abstract RiaNum add(RatNum num);

    public abstract RiaNum add(BigInteger num);

    public abstract RiaNum mul(RiaNum num);

    public abstract RiaNum mul(long num);

    public abstract RiaNum mul(RatNum num);

    public abstract RiaNum mul(BigInteger num);

    public abstract RiaNum div(RiaNum num);

    public abstract RiaNum div(long num);

    public abstract RiaNum divFrom(long num);

    public abstract RiaNum divFrom(RatNum num);

    public abstract RiaNum intDiv(RiaNum num);

    public abstract RiaNum intDiv(int num);

    public abstract RiaNum intDivFrom(long num);

    public abstract RiaNum intDivFrom(BigInteger num);

    public abstract RiaNum rem(RiaNum num);

    public abstract RiaNum rem(int num);

    public abstract RiaNum remFrom(long num);

    public abstract RiaNum remFrom(BigInteger num);

    public abstract RiaNum sub(RiaNum num);

    public abstract RiaNum sub(long num);

    public abstract RiaNum subFrom(long num);

    public abstract RiaNum subFrom(RatNum num);

    public abstract RiaNum subFrom(BigInteger num);

    public abstract RiaNum and(RiaNum num);

    public abstract RiaNum and(BigInteger num);

    public abstract RiaNum or(RiaNum num);

    public abstract RiaNum or(long num);

    public abstract RiaNum xor(RiaNum num);

    public abstract RiaNum xor(long num);

    public abstract int rCompare(long num);

    public abstract int rCompare(RatNum num);

    public abstract int rCompare(BigInteger num);

    public RiaNum shl(int by) {
        if(by < 0) {
            return new IntNum(longValue() >>> -by);
        }
        long l, v;
        if(by < 32 && (v = longValue()) < (l = SHL_LIMIT[by]) && v > -l) {
            return new IntNum(v << by);
        }
        return new BigNum(toBigInteger().shiftLeft(by));
    }

    public boolean equals(Object x) {
        return x instanceof RiaNum && compareTo(x) == 0;
    }

    public String toString(int radix, int format) {
        return Long.toString(longValue(), radix);
    }

    public BigInteger toBigInteger() {
        return BigInteger.valueOf(longValue());
    }

    public BigDecimal toBigDecimal() {
        return new BigDecimal(toString());
    }
}
