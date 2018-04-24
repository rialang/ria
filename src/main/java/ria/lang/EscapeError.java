package ria.lang;

final class EscapeError extends Error {
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
