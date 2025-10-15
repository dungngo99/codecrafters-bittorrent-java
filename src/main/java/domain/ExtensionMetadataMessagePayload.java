package domain;

import java.util.Map;

public class ExtensionMetadataMessagePayload {
    private byte peerMessageId;
    private Map<String, ValueWrapper> extensionMetadataMap;

    public byte getPeerMessageId() {
        return peerMessageId;
    }

    public void setPeerMessageId(byte peerMessageId) {
        this.peerMessageId = peerMessageId;
    }

    public Map<String, ValueWrapper> getExtensionMetadataMap() {
        return extensionMetadataMap;
    }

    public void setExtensionMetadataMap(Map<String, ValueWrapper> extensionMetadataMap) {
        this.extensionMetadataMap = extensionMetadataMap;
    }
}
