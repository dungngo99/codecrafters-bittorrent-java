package domain;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private final int status;
    private final Map<String, String> headers;
    private final byte[] bytes;

    public HttpResponse(int status, byte[] bytes) {
        this.status = status;
        this.headers = new HashMap<>();
        this.bytes = bytes;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
