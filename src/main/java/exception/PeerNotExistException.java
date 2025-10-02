package exception;

public class PeerNotExistException extends RuntimeException {

    public PeerNotExistException(String message) {
        super(message);
    }
}
