package ria.lang;

import java.util.Random;

@SuppressWarnings("unused")
class RandomInt extends Fun {
    private static Random rnd = null;

    @Override
    public Object apply(Object x) {
        Random rnd = initRandom();
        RiaNum n = (RiaNum) x;
        if (n.rCompare(0x7fffffffL) > 0) {
            return new IntNum(rnd.nextInt(n.intValue()));
        }
        if (n.rCompare(Long.MAX_VALUE) > 0) {
            return new IntNum((long) (n.doubleValue() * rnd.nextDouble()));
        }
        // XXX
        return new FloatNum(Math.floor(n.doubleValue() * rnd.nextDouble()));
    }

    static synchronized Random initRandom() {
        if (rnd == null) {
            rnd = new Random();
        }
        return rnd;
    }
}
