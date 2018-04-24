package ria.lang;

class Unsafe {
    public static RuntimeException unsafeThrow(Throwable var0) {
        if ( var0 == null ) {
            throw new NullPointerException("var0");
        }
        Unsafe.sneakyThrow0(var0);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow0(Throwable t) throws T {
        throw (T) t;
    }
}

