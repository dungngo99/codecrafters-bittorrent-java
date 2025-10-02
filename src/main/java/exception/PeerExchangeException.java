package exception;

public class PeerExchangeException extends RuntimeException {

    public PeerExchangeException(Exception e) {
        super(e);
    }
}
