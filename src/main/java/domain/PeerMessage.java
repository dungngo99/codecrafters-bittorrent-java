package domain;

public class PeerMessage {
    private int prefixedLength;
    private byte messageId;
    private byte[] payload;

    public int getPrefixedLength() {
        return prefixedLength;
    }

    public void setPrefixedLength(int prefixedLength) {
        this.prefixedLength = prefixedLength;
    }

    public byte getMessageId() {
        return messageId;
    }

    public void setMessageId(byte messageId) {
        this.messageId = messageId;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "PeerMessage{" +
                "prefixedLength=" + prefixedLength +
                ", messageId=" + messageId +
                '}';
    }
}
