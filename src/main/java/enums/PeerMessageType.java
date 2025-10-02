package enums;

/**
 * The possible values are:
 * 0 - choke
 * 1 - unchoke
 * 2 - interested
 * 3 - not interested
 * 4 - have
 * 5 - bitfield
 * 6 - request
 * 7 - piece
 * 8 - cancel
 */
public enum PeerMessageType {
    CHOKE(0),
    UNCHOKE(1),
    INTERESTED(2),
    NOT_INTERESTED(3),
    HAVE(4),
    BITFIELD(5),
    REQUEST(6),
    PIECE(7),
    CANCEL(8);

    private final int value;

    PeerMessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
