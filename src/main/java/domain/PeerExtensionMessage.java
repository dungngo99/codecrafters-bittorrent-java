package domain;

import java.util.Map;

public class PeerExtensionMessage {
    private byte messageId;
    private Map<String, Integer> extensionNameIdMap;

    public byte getMessageId() {
        return messageId;
    }

    public void setMessageId(byte messageId) {
        this.messageId = messageId;
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
                "messageId=" + messageId +
                ", extensionNameIdMap=" + extensionNameIdMap +
                '}';
    }
}
