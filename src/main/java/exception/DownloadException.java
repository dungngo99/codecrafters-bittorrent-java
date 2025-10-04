package exception;

public class DownloadException extends RuntimeException {

    public DownloadException(Exception e) {
        super(e);
    }
}
