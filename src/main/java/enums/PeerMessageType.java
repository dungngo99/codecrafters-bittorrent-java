package enums;

public enum PeerMessageType {
    OWNING_PIECE(5),
    INTERESTED(2),
    UNCHOKED(1),
    BLOCK_REQUEST(6),
    BLOCK_RESPONSE(7);

    private final int value;

    PeerMessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
