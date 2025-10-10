package domain;

public class PeerRequestQueryParam {
    private String trackerUrl;
    private String infoHash;
    private String infoLength;
    private String peerId;

    public String getTrackerUrl() {
        return trackerUrl;
    }

    public void setTrackerUrl(String trackerUrl) {
        this.trackerUrl = trackerUrl;
    }

    public String getInfoHash() {
        return infoHash;
    }

    public void setInfoHash(String infoHash) {
        this.infoHash = infoHash;
    }

    public String getInfoLength() {
        return infoLength;
    }

    public void setInfoLength(String infoLength) {
        this.infoLength = infoLength;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }
}
