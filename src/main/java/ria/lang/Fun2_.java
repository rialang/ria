package ria.lang;

class Fun2_ extends Fun {
    private final Fun2 fun;
    private final Object arg;

    Fun2_(Fun2 var1, Object var2) {
        this.fun = var1;
        this.arg = var2;
    }

    @Override
    public Object apply(Object var1) {
        return this.fun.apply(this.arg, var1);
    }
}
