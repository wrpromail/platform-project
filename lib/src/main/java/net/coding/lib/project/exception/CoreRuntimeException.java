package net.coding.lib.project.exception;

public class CoreRuntimeException extends RuntimeException {

    private final CoreException.ExceptionType exceptionType;

    public CoreRuntimeException(CoreException.ExceptionType exceptionType) {
        this.exceptionType = exceptionType;
    }

    public CoreRuntimeException(CoreException.ExceptionType exceptionType, String message) {
        super(message);
        this.exceptionType = exceptionType;
        this.exceptionType.setArgs(new Object[]{message});
    }

    public CoreRuntimeException(CoreException.ExceptionType exceptionType, String message, Throwable cause) {
        super(message, cause);
        this.exceptionType = exceptionType;
    }

    public CoreRuntimeException(CoreException.ExceptionType exceptionType, Throwable cause) {
        super(cause);
        this.exceptionType = exceptionType;
    }

    public CoreException.ExceptionType getExceptionType() {
        return this.exceptionType;
    }
}
