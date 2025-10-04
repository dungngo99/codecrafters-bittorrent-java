package exception;

public class DownloadPieceException extends RuntimeException {

    public DownloadPieceException(Exception e) {
        super(e);
    }
}
