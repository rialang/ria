package ria.lang;

import java.math.BigInteger;

public final class RatNum extends RiaNum {
    private final long numerator;
    private final long denominator;

    public RatNum(int numerator, int denominator) {
        if (denominator == 0) {
            throw new IllegalArgumentException(numerator + "/0");
        }
        if (denominator < 0) {
            this.numerator = -(long) numerator;
            this.denominator = -(long) denominator;
        } else {
            this.numerator = numerator;
            this.denominator = denominator;
        }
    }

    private RatNum(long numerator, long denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    @Override
    public RiaNum add(RiaNum num) {
        return num.add(this);
    }

    private static long gcd(long a, long b) {
        long c;
        while (b != 0) {
            b = a % (c = b);
            a = c;
        }
        return a;
    }

    @Override
    public RiaNum add(long num) {
        long a, c, gcd;
        if (num > 0x7fffffffL || num < -0x7fffffffL ||
            (a = num * denominator) > 0x7fffffff7fffffffL ||
             a < -0x7fffffff7fffffffL) {
            return new FloatNum((double) numerator / denominator + num);
        }
        if ((c = a + numerator) > 0x7fffffffL || c < -0x7fffffff) {
            long d = denominator / (gcd = gcd(c < 0 ? -c : c, denominator));
            if ((c /= gcd) > 0x7fffffffL || c < -0x7fffffffL) {
                return new FloatNum((double) c / d);
            }
            return new RatNum(c, d);
        }
        return new RatNum(c, denominator);
    }

    @Override
    public RiaNum add(RatNum num) {
        long a, b = numerator * num.denominator, c, gcd;
        if ((a = num.numerator * denominator) > 0
                ? a > 0x3fffffffffffffffL || b > 0x3fffffffffffffffL
                : a < -0x3fffffffffffffffL || b < -0x3fffffffffffffffL) {
            return new FloatNum((double) numerator / denominator +
                (double) num.numerator / num.denominator);
        }
        long d = denominator * num.denominator;
        if ((c = a + b) > 0x7fffffffL || c < -0x7fffffff || 
            d > 0x7ffffffffL || d < -0x7fffffffL) {
            d /= gcd = gcd(c < 0 ? -c : c, d);
            if ((c /= gcd) > 0x7fffffffL || c < -0x7fffffffL ||
                d > 0x7fffffffL || d < -0x7fffffffL) {
                return new FloatNum((double) c / d);
            }
        }
        return new RatNum(c, d);
    }

    @Override
    public RiaNum add(BigInteger num) {
        return new FloatNum((double) numerator / denominator
                            + num.doubleValue());
    }

    @Override
    public RiaNum mul(RiaNum num) {
        return num.mul(this);
    }

    @Override
    public RiaNum mul(long num) {
        long a;
        if (num > 0x7fffffffL || num < -0x7fffffffL) {
            return new FloatNum((double) numerator / denominator * num);
        }
        if ((a = numerator * num) > 0x7fffffffL || a < -0x7fffffffL) {
            long gcd, b = denominator / (gcd = gcd(a, denominator));
            if ((a /= gcd) > 0x7fffffffL || a < -0x7fffffffL) {
                return new FloatNum((double) a / b);
            }
            return new RatNum(a, b);
        }
        return new RatNum(a, denominator);
    }

    @Override
    public RiaNum mul(RatNum num) {
        long a, b = denominator * num.denominator, gcd;
        if ((a = numerator * num.numerator) > 0x7fffffffL
            || a < -0x7fffffffL || b > 0x7fffffffL || b < -0x7fffffff) {
            b /= gcd = gcd(a, b);
            if ((a /= gcd) > 0x7fffffffL || a < -0x7fffffffL ||
                b > 0x7fffffffL || b < -0x7fffffffL) {
                return new FloatNum((double) a / b);
            }
        }
        return new RatNum(a, b);
    }

    @Override
    public RiaNum mul(BigInteger num) {
        return new FloatNum((double) numerator / denominator
                            * num.doubleValue());
    }

    @Override
    public RiaNum div(RiaNum num) {
        return num.divFrom(this);
    }

    @Override
    public RiaNum div(long num) {
        long a;
        if (num > 0x7fffffffL || num < -0x7fffffffL) {
            return new FloatNum((double) numerator /
                        ((double) denominator * num));
        }
        if ((a = denominator * num) > 0x7fffffffL || a < -0x7fffffffL) {
            long gcd, b = numerator / (gcd = gcd(a, numerator));
            if ((a /= gcd) > 0x7fffffffL || a < -0x7fffffffL) {
                return new FloatNum((double) b / a);
            }
            return new RatNum(b, a);
        }
        return new RatNum(numerator, a);
    }

    // num / this
    @Override
    public RiaNum divFrom(long num) {
        long a;
        if (num > 0x7fffffffL || num < -0x7fffffffL) {
            return new FloatNum((double) num / numerator * denominator);
        }
        if ((a = denominator * num) > 0x7fffffffL || a < -0x7fffffffL) {
            long gcd, b = numerator / (gcd = gcd(a, numerator));
            if ((a /= gcd) > 0x7fffffffL || a < -0x7fffffffL) {
                return new FloatNum((double) a / b);
            }
            return new RatNum(a, b);
        }
        return new RatNum(a, numerator);
    }

    @Override
    public RiaNum divFrom(RatNum num) {
        long a, b = numerator * num.denominator, gcd;
        if ((a = denominator * num.numerator) > 0x7fffffffL
            || a < -0x7fffffffL || b > 0x7fffffffL || b < -0x7fffffff) {
            b /= gcd = gcd(a, b);
            if ((a /= gcd) > 0x7fffffffL || a < -0x7fffffffL ||
                b > 0x7fffffffL || b < -0x7fffffffL) {
                return new FloatNum((double) a / b);
            }
        }
        return new RatNum(a, b);
    }

    @Override
    public RiaNum intDiv(RiaNum num) {
        return num.intDivFrom(numerator / denominator);
    }

    @Override
    public RiaNum intDiv(int num) {
        return new IntNum(numerator / denominator / num);
    }

    @Override
    public RiaNum intDivFrom(long num) {
        return new IntNum(num / (numerator / denominator));
    }

    @Override
    public RiaNum intDivFrom(BigInteger num) {
        return new BigNum(num.divide(
                            BigInteger.valueOf(numerator / denominator)));
    }

    @Override
    public RiaNum rem(RiaNum num) {
        return num.remFrom(numerator / denominator);
    }

    @Override
    public RiaNum rem(int num) {
        return new IntNum((numerator / denominator) % num);
    }

    @Override
    public RiaNum remFrom(long num) {
        return new IntNum(num % (numerator / denominator));
    }

    @Override
    public RiaNum remFrom(BigInteger num) {
        return new BigNum(num.remainder(
                            BigInteger.valueOf(numerator / denominator)));
    }

    @Override
    public RiaNum sub(RiaNum num) {
        return num.subFrom(this);
    }

    @Override
    public RiaNum sub(long num) {
        return add(-num);
    }

    @Override
    public RiaNum subFrom(long num) {
        long a, c, gcd;
        if (num > 0x7fffffffL || num < -0x7fffffffL ||
            (a = num * denominator) > 0x7fffffff7fffffffL ||
             a < -0x7fffffff7fffffffL) {
            return new FloatNum((double) num -
                (double) numerator / denominator);
        }
        if ((c = a - numerator) > 0x7fffffffL || c < -0x7fffffff) {
            long d = denominator / (gcd = gcd(c < 0 ? -c : c, denominator));
            if ((c /= gcd) > 0x7fffffffL || c < -0x7fffffffL) {
                return new FloatNum((double) c / d);
            }
            return new RatNum(c, d);
        }
        return new RatNum(c, denominator);
    }

    @Override
    public RiaNum subFrom(RatNum num) {
        long a, b = numerator * num.denominator, c, gcd;
        if ((a = num.numerator * denominator) > 0
                ? a > 0x3fffffffffffffffL || b < -0x3fffffffffffffffL
                : a < -0x3fffffffffffffffL || b > 0x3fffffffffffffffL) {
            return new FloatNum((double) numerator / denominator +
                (double) num.numerator / num.denominator);
        }
        long d = denominator * num.denominator;
        if ((c = a - b) > 0x7fffffffL || c < -0x7fffffff || 
            d > 0x7ffffffffL || d < -0x7fffffffL) {
            d /= gcd = gcd(c < 0 ? -c : c, d);
            if ((c /= gcd) > 0x7fffffffL || c < -0x7fffffffL ||
                d > 0x7fffffffL || d < -0x7fffffffL) {
                return new FloatNum((double) c / d);
            }
        }
        return new RatNum(c, d);
    }

    @Override
    public RiaNum and(RiaNum num) {
        return new IntNum(num.longValue() & (numerator / denominator));
    }

    @Override
    public RiaNum and(BigInteger num) {
        return new IntNum(num.longValue() & (numerator / denominator));
    }

    @Override
    public RiaNum or(RiaNum num) {
        return num.or(numerator / denominator);
    }

    @Override
    public RiaNum or(long num) {
        return new IntNum(num | (numerator / denominator));
    }

    @Override
    public RiaNum xor(RiaNum num) {
        return num.xor(numerator / denominator);
    }

    @Override
    public RiaNum xor(long num) {
        return new IntNum(num ^ (numerator / denominator));
    }

    public RatNum reduce() {
        long gcd = gcd(numerator, denominator);
        return new RatNum(numerator / gcd, denominator / gcd);
    }

    @Override
    public byte byteValue() {
        return (byte) (numerator / denominator);
    }

    @Override
    public short shortValue() {
        return (short) (numerator / denominator);
    }

    @Override
    public int intValue() {
        return (int) (numerator / denominator);
    }

    @Override
    public long longValue() {
        return numerator / denominator;
    }

    @Override
    public float floatValue() {
        return (float) ((double) numerator / denominator);
    }

    @Override
    public double doubleValue() {
        return (double) numerator / (double) denominator;
    }

    public static RiaNum div(long numerator, long denominator) {
        if (denominator == 0) {
            throw new IllegalArgumentException("division by zero");
        }
        if (numerator > 0x7fffffff || numerator < -0x7fffffff ||
            denominator > 0x7fffffff || denominator < -0x7fffffff) {
            long gcd;
            denominator /= gcd = gcd(numerator, denominator);
            if ((numerator /= gcd) > 0x7fffffff || numerator < -0x7fffffff ||
                denominator > 0x7fffffff || denominator < -0x7fffffff) {
                return new FloatNum((double) numerator / denominator);
            }
        }
        return denominator < 0 ? new RatNum(-numerator, -denominator)
                               : new RatNum(numerator, denominator);
    }

    @Override
    public RiaNum subFrom(BigInteger num) {
        return new FloatNum((double) numerator / denominator
                            - num.doubleValue());
    }

    public int numerator() {
        return (int) numerator;
    }

    public int denominator() {
        return (int) denominator;
    }

    @Override
    public int compareTo(Object num) {
        return ((RiaNum) num).rCompare(this);
    }

    @Override
    public int rCompare(long num) {
        if (-0x7fffffff <= num && num <= 0x7fffffff) {
            long x = num * denominator;
            return Long.compare(x, numerator);
        }
        return (double) numerator / denominator < (double) num ? 1 : -1;
    }

    @Override
    public int rCompare(RatNum num) {
        long a = numerator * num.denominator;
        long b = num.numerator * denominator;
        return Long.compare(b, a);
    }

    @Override
    public int rCompare(BigInteger num) {
        if (numerator % denominator == 0 &&
            BigInteger.valueOf(numerator / denominator).equals(num)) {
            return 0;
        }
        double a = (double) numerator / denominator, b = num.doubleValue();
        return Double.compare(b, a);
    }

    public String toString() {
        if (numerator % denominator == 0) {
            return Integer.toString((int) numerator / (int) denominator);
        }
        return Double.toString((double) numerator / (double) denominator);
    }

    public int hashCode() {
        long x = numerator / denominator;
        long d = Double.doubleToLongBits((double) numerator / denominator - x);
        if (d != 0x8000000000000000L) {
            x ^= d;
        }
        return (int) (x ^ (x >>> 32));
    }
}
