package ria.lang;

import java.math.BigInteger;

public final class FloatNum extends RiaNum {
    private final double v;

    public FloatNum(double num) {
        v = num;
    }

    @Override
    public RiaNum add(RiaNum num) {
        return new FloatNum(v + num.doubleValue());
    }

    @Override
    public RiaNum add(long num) {
        return new FloatNum(v + num);
    }

    @Override
    public RiaNum add(RatNum num) {
        return new FloatNum(v + num.doubleValue());
    }

    @Override
    public RiaNum add(BigInteger num) {
        return new FloatNum(v + num.doubleValue());
    }

    @Override
    public RiaNum mul(RiaNum num) {
        return new FloatNum(v * num.doubleValue());
    }

    @Override
    public RiaNum mul(long num) {
        return new FloatNum(v * num);
    }

    @Override
    public RiaNum mul(RatNum num) {
        return new FloatNum(v * num.doubleValue());
    }

    @Override
    public RiaNum mul(BigInteger num) {
        return new FloatNum(v * num.doubleValue());
    }

    @Override
    public RiaNum div(RiaNum num) {
        return new FloatNum(v / num.doubleValue());
    }

    @Override
    public RiaNum div(long num) {
        return new FloatNum(v / num);
    }

    @Override
    public RiaNum divFrom(long num) {
        return new FloatNum(num / v);
    }

    @Override
    public RiaNum divFrom(RatNum num) {
        return new FloatNum(num.doubleValue() / v);
    }

    @Override
    public RiaNum intDiv(RiaNum num) {
        double res = (v >= 0 ? Math.floor(v) : Math.ceil(v)) /
            num.doubleValue();
        return res > 2147483647.0 || res < -2147483647.0
            ? new FloatNum(res >= 0 ? Math.floor(res) : Math.ceil(res))
            : new IntNum((long)res);
    }

    @Override
    public RiaNum intDiv(int num) {
        double res = (v >= 0 ? Math.floor(v) : Math.ceil(v)) / num;
        return res > 2147483647.0 || res < -2147483647.0
            ? new FloatNum(res >= 0 ? Math.floor(res) : Math.ceil(res))
            : new IntNum((long)res);
    }

    @Override
    public RiaNum intDivFrom(long num) {
        return new IntNum((long)
            (num / (v >= 0 ? Math.floor(v) : Math.ceil(v))));
    }

    @Override
    public RiaNum intDivFrom(BigInteger num) {
        // XXX
        double res = num.doubleValue() /
            (v >= 0 ? Math.floor(v) : Math.ceil(v));
        return res > 2147483647.0 || res < -2147483647.0
            ? new FloatNum(res >= 0 ? Math.floor(res) : Math.ceil(res))
            : new IntNum((long)res);
    }

    @Override
    public RiaNum rem(RiaNum num) {
        return new IntNum((long)v % num.longValue());
    }

    @Override
    public RiaNum rem(int num) {
        return new IntNum((long)v % num);
    }

    @Override
    public RiaNum remFrom(long num) {
        return new IntNum(num % (long)v);
    }

    @Override
    public RiaNum remFrom(BigInteger num) {
        // XXX
        double res = num.doubleValue() %
            (v >= 0 ? Math.floor(v) : Math.ceil(v));
        return res > 2147483647.0 || res < -2147483647.0
            ? new FloatNum(res >= 0 ? Math.floor(res) : Math.ceil(res))
            : new IntNum((long)res);
    }

    @Override
    public RiaNum sub(RiaNum num) {
        return new FloatNum(v - num.doubleValue());
    }

    @Override
    public RiaNum sub(long num) {
        return new FloatNum(v - num);
    }

    @Override
    public RiaNum subFrom(long num) {
        return new FloatNum(num - v);
    }

    @Override
    public RiaNum subFrom(RatNum num) {
        return new FloatNum(num.doubleValue() - v);
    }

    @Override
    public RiaNum subFrom(BigInteger num) {
        return new FloatNum(num.doubleValue() - v);
    }

    @Override
    public RiaNum and(RiaNum num) {
        return new IntNum(num.longValue() & (long)v);
    }

    @Override
    public RiaNum and(BigInteger num) {
        return new IntNum(num.longValue() & (long)v);
    }

    @Override
    public RiaNum or(RiaNum num) {
        return num.or((long)v);
    }

    @Override
    public RiaNum or(long num) {
        return new IntNum(num | (long)v);
    }

    @Override
    public RiaNum xor(RiaNum num) {
        return num.xor((long)v);
    }

    @Override
    public RiaNum xor(long num) {
        return new IntNum(num ^ (long)v);
    }

    @Override
    public byte byteValue() {
        return (byte)v;
    }

    @Override
    public short shortValue() {
        return (short)v;
    }

    @Override
    public int intValue() {
        return (int)v;
    }

    @Override
    public long longValue() {
        return (long)v;
    }

    @Override
    public float floatValue() {
        return (float)v;
    }

    @Override
    public double doubleValue() {
        return v;
    }

    @Override
    public int compareTo(Object num) {
        double x = ((Number)num).doubleValue();
        return Double.compare(v, x);
    }

    @Override
    public int rCompare(long num) {
        return v < num ? 1 : v > num ? -1 : 0;
    }

    @Override
    public int rCompare(RatNum num) {
        double x = num.doubleValue();
        return Double.compare(x, v);
    }

    @Override
    public int rCompare(BigInteger num) {
        double x = num.doubleValue();
        return Double.compare(x, v);
    }

    public String toString() {
        return Double.toString(v);
    }

    public int hashCode() {
        // hashCode must be same when equals is same
        // a bit rough, but hopefully it satisfies that condition ;)
        long x = (long)v;
        long d = Double.doubleToLongBits(v - x);
        if(d != 0x8000000000000000L) {
            x ^= d;
        }
        return (int)(x ^ (x >>> 32));
    }

    public boolean equals(Object num) {
        // It returns false when comparing two NaNs, however this should still
        // follow the hashCode/equals contract as it is allowed to have same
        // hashCode for values not equal. A bit weirdness can happen, like not
        // founding NaN from hash containing it, but fixing it isn't probably
        // worth the performance overhead of making equals more complicated.
        // Just avoid using NaN's, they are messed up in IEEE (and therefore
        // JVM) floats.
        return num instanceof RiaNum && v == ((RiaNum)num).doubleValue();
    }
}
