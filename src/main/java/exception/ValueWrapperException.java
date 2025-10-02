package exception;

public class ValueWrapperException extends RuntimeException {

    public ValueWrapperException(String message) {
        super(message);
    }

    public ValueWrapperException(Throwable e) {
        super(e);
    }
}
