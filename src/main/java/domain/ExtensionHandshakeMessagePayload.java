package domain;

import java.util.Map;

public class ExtensionHandshakeMessagePayload {
    private byte peerMessageId;
    private Map<String, Integer> extensionNameIdMap;

    public byte getPeerMessageId() {
        return peerMessageId;
    }

    public void setPeerMessageId(byte peerMessageId) {
        this.peerMessageId = peerMessageId;
    }

    public Map<String, Integer> getExtensionNameIdMap() {
        return extensionNameIdMap;
    }

    public void setExtensionNameIdMap(Map<String, Integer> extensionNameIdMap) {
        this.extensionNameIdMap = extensionNameIdMap;
    }

    @Override
    public String toString() {
        return "PeerExtensionMessage{" +
                "peerMessageId=" + peerMessageId +
                ", extensionNameIdMap=" + extensionNameIdMap +
                '}';
    }
}
