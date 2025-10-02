package exception;

public class InvalidCmdException extends RuntimeException {

    public InvalidCmdException(String message) {
        super(message);
    }
}
