package ria.lang;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class BigNum extends RiaNum {
    private final BigInteger v;

    public BigNum(long num) {
        v = BigInteger.valueOf(num);
    }

    public BigNum(BigInteger num) {
        v = num;
    }

    public BigNum(String num, int radix) {
        v = new BigInteger(num, radix);
    }

    @Override
    public RiaNum add(RiaNum num) {
        return num.add(v);
    }

    @Override
    public RiaNum add(RatNum num) {
        return num.add(v);
    }

    @Override
    public RiaNum add(BigInteger num) {
        return new BigNum(num.add(v));
    }

    @Override
    public RiaNum add(long num) {
        return new BigNum(v.add(BigInteger.valueOf(num)));
    }

    @Override
    public RiaNum mul(RiaNum num) {
        return num.mul(v);
    }

    @Override
    public RiaNum mul(long num) {
        return new BigNum(v.multiply(BigInteger.valueOf(num)));
    }

    @Override
    public RiaNum mul(RatNum num) {
        return num.mul(v);
    }

    @Override
    public RiaNum mul(BigInteger num) {
        return new BigNum(num.multiply(v));
    }

    @Override
    public RiaNum div(RiaNum num) {
        return new FloatNum(v.doubleValue() / num.doubleValue());
    }

    @Override
    public RiaNum div(long num) {
        return new FloatNum(v.doubleValue() / num);
    }

    @Override
    public RiaNum divFrom(long num) {
        return new FloatNum((double)num / v.doubleValue());
    }

    @Override
    public RiaNum divFrom(RatNum num) {
        return new FloatNum(num.doubleValue() / v.doubleValue());
    }

    @Override
    public RiaNum intDiv(RiaNum num) {
        return num.intDivFrom(v);
    }

    @Override
    public RiaNum intDiv(int num) {
        return new BigNum(v.divide(BigInteger.valueOf(num)));
    }

    @Override
    public RiaNum intDivFrom(BigInteger num) {
        return new BigNum(num.divide(v));
    }

    @Override
    public RiaNum intDivFrom(long num) {
        return new IntNum(BigInteger.valueOf(num).divide(v).longValue());
    }

    @Override
    public RiaNum rem(RiaNum num) {
        return num.remFrom(v);
    }

    @Override
    public RiaNum rem(int num) {
        return new IntNum(v.remainder(BigInteger.valueOf(num)).longValue());
    }

    @Override
    public RiaNum remFrom(BigInteger num) {
        return new BigNum(num.remainder(v));
    }

    @Override
    public RiaNum remFrom(long num) {
        return new IntNum(BigInteger.valueOf(num).remainder(v).longValue());
    }

    @Override
    public RiaNum sub(RiaNum num) {
        return num.subFrom(v);
    }

    @Override
    public RiaNum sub(long num) {
        return new BigNum(v.subtract(BigInteger.valueOf(num)));
    }

    @Override
    public RiaNum subFrom(long num) {
        return new BigNum(BigInteger.valueOf(num).subtract(v));
    }

    @Override
    public RiaNum subFrom(RatNum num) {
        return new FloatNum(num.doubleValue() - v.doubleValue());
    }

    @Override
    public RiaNum subFrom(BigInteger num) {
        return new BigNum(num.subtract(v));
    }

    @Override
    public RiaNum shl(int num) {
        return new BigNum(v.shiftLeft(num));
    }

    @Override
    public RiaNum and(RiaNum num) {
        return num.and(v);
    }

    @Override
    public RiaNum and(BigInteger num) {
        return new BigNum(v.and(num));
    }

    @Override
    public RiaNum or(RiaNum num) {
        return new BigNum(v.or(num.toBigInteger()));
    }

    @Override
    public RiaNum or(long num) {
        return new BigNum(v.or(BigInteger.valueOf(num)));
    }

    @Override
    public RiaNum xor(RiaNum num) {
        return new BigNum(v.xor(num.toBigInteger()));
    }

    @Override
    public RiaNum xor(long num) {
        return new BigNum(v.xor(BigInteger.valueOf(num)));
    }

    @Override
    public byte byteValue() {
        return v.byteValue();
    }

    @Override
    public short shortValue() {
        return v.shortValue();
    }

    @Override
    public int intValue() {
        return v.intValue();
    }

    @Override
    public long longValue() {
        return v.longValue();
    }

    @Override
    public float floatValue() {
        return v.floatValue();
    }

    @Override
    public double doubleValue() {
        return v.doubleValue();
    }

    @Override
    public BigInteger toBigInteger() {
        return v;
    }

    @Override
    public BigDecimal toBigDecimal() {
        return new BigDecimal(v);
    }

    @Override
    public int compareTo(Object num) {
        return ((RiaNum)num).rCompare(v);
    }

    @Override
    public int rCompare(long num) {
        return BigInteger.valueOf(num).compareTo(v);
    }

    @Override
    public int rCompare(RatNum num) {
        return -num.rCompare(v);
    }

    @Override
    public int rCompare(BigInteger num) {
        return num.compareTo(v);
    }

    public String toString() {
        return v.toString();
    }

    @Override
    public String toString(int radix, int format) {
        return v.toString(radix);
    }

    public int hashCode() {
        if(v.bitLength() > 63) {
            return v.hashCode();
        }
        // for values in long's range return same as IntNum or Long
        long x = v.longValue();
        return (int)(x ^ (x >>> 32));
    }
}
