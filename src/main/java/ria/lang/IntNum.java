package ria.lang;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class IntNum extends RiaNum {
    public static final IntNum __2 = new IntNum(-2);
    public static final IntNum __1 = new IntNum(-1);
    @SuppressWarnings("unused")
    public static final IntNum _0 = new IntNum(0);
    @SuppressWarnings("unused")
    public static final IntNum _1 = new IntNum(1);
    @SuppressWarnings("unused")
    public static final IntNum _2 = new IntNum(2);
    @SuppressWarnings("unused")
    public static final IntNum _3 = new IntNum(3);
    @SuppressWarnings("unused")
    public static final IntNum _4 = new IntNum(4);
    @SuppressWarnings("unused")
    public static final IntNum _5 = new IntNum(5);
    @SuppressWarnings("unused")
    public static final IntNum _6 = new IntNum(6);
    @SuppressWarnings("unused")
    public static final IntNum _7 = new IntNum(7);
    @SuppressWarnings("unused")
    public static final IntNum _8 = new IntNum(8);
    public static final IntNum _9 = new IntNum(9);

    private final long v;

    public IntNum(int num) {
        v = num;
    }

    public IntNum(long num) {
        v = num;
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
    public RiaNum add(long num) {
        if(num > 0 ? num > 0x3fffffffffffffffL || v > 0x3fffffffffffffffL
            : num < -0x3fffffffffffffffL || v < -0x3fffffffffffffffL) {
            return new BigNum(BigInteger.valueOf(v).add(
                BigInteger.valueOf(num)));
        }
        return new IntNum(v + num);
    }

    @Override
    public RiaNum add(BigInteger num) {
        return new BigNum(BigInteger.valueOf(v).add(num));
    }

    @Override
    public RiaNum mul(RiaNum num) {
        return num.mul(v);
    }

    @Override
    public RiaNum mul(long num) {
        if(num < -0x7fffffffL || num > 0x7fffffffL
            || v < -0x7fffffffL || v > 0x7fffffffL) {
            return new BigNum(BigInteger.valueOf(v).multiply(
                BigInteger.valueOf(num)));
        }
        return new IntNum(v * num);
    }

    @Override
    public RiaNum mul(BigInteger num) {
        return new BigNum(BigInteger.valueOf(v).multiply(num));
    }

    @Override
    public RiaNum mul(RatNum num) {
        return num.mul(v);
    }

    @Override
    public RiaNum div(RiaNum num) {
        return num.divFrom(v);
    }

    @Override
    public RiaNum div(long num) {
        return RatNum.div(v, num);
    }

    @Override
    public RiaNum divFrom(long num) {
        return RatNum.div(num, v);
    }

    @Override
    public RiaNum divFrom(RatNum num) {
        return num.div(v);
    }

    @Override
    public RiaNum intDiv(RiaNum num) {
        return num.intDivFrom(v);
    }

    @Override
    public RiaNum intDiv(int num) {
        return new IntNum(v / num);
    }

    @Override
    public RiaNum intDivFrom(long num) {
        return new IntNum(num / v);
    }

    @Override
    public RiaNum intDivFrom(BigInteger num) {
        return new BigNum(num.divide(BigInteger.valueOf(v)));
    }

    @Override
    public RiaNum rem(RiaNum num) {
        return num.remFrom(v);
    }

    @Override
    public RiaNum rem(int num) {
        return new IntNum(v % num);
    }

    @Override
    public RiaNum remFrom(long num) {
        return new IntNum(num % v);
    }

    @Override
    public RiaNum remFrom(BigInteger num) {
        return new BigNum(num.remainder(BigInteger.valueOf(v)));
    }

    @Override
    public RiaNum sub(RiaNum num) {
        return num.subFrom(v);
    }

    @Override
    public RiaNum sub(long num) {
        long n;
        if(num < 0 ? num < -0x3fffffffffffffffL || v > 0x3fffffffffffffffL
            : num > 0x3fffffffffffffffL || v < -0x3fffffffffffffffL) {
            return new BigNum(BigInteger.valueOf(v).subtract(
                BigInteger.valueOf(num)));
        }
        return new IntNum(v - num);
    }

    @Override
    public RiaNum subFrom(long num) {
        if(num < 0 ? num < -0x3fffffffffffffffL || v > 0x3fffffffffffffffL
            : num > 0x3fffffffffffffffL || v < -0x3fffffffffffffffL) {
            return new BigNum(BigInteger.valueOf(num).subtract(
                BigInteger.valueOf(v)));
        }
        return new IntNum(num - v);
    }

    @Override
    public RiaNum subFrom(RatNum num) {
        return num.sub(v);
    }

    @Override
    public RiaNum subFrom(BigInteger num) {
        return new BigNum(num.subtract(BigInteger.valueOf(v)));
    }

    @Override
    public RiaNum and(RiaNum num) {
        return new IntNum(num.longValue() & v);
    }

    @Override
    public RiaNum and(BigInteger num) {
        return new IntNum(num.longValue() & v);
    }

    @Override
    public RiaNum or(RiaNum num) {
        return num.or(v);
    }

    @Override
    public RiaNum or(long num) {
        return new IntNum(num | v);
    }

    @Override
    public RiaNum xor(RiaNum num) {
        return num.xor(v);
    }

    @Override
    public RiaNum xor(long num) {
        return new IntNum(num ^ v);
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
        return v;
    }

    @Override
    public float floatValue() {
        return v;
    }

    @Override
    public double doubleValue() {
        return v;
    }

    @Override
    public int compareTo(Object num) {
        return ((RiaNum)num).rCompare(v);
    }

    @Override
    public int rCompare(long num) {
        return Long.compare(num, v);
    }

    @Override
    public int rCompare(RatNum num) {
        return -num.rCompare(v);
    }

    @Override
    public int rCompare(BigInteger num) {
        return num.compareTo(BigInteger.valueOf(v));
    }

    @Override
    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(v);
    }

    public String toString() {
        return Long.toString(v);
    }

    public int hashCode() {
        return (int)(v ^ (v >>> 32));
    }
}
