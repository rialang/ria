package ria.lang;

@SuppressWarnings("unused")
public class ExitError extends ThreadDeath {
    private final int exitCode;

    public ExitError(int exitCode) {
    	this.exitCode = exitCode;
    }

    public int getExitCode() {
    	return exitCode;
    }
}
