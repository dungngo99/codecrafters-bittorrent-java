package enums;

public enum ExtensionMessageType {
    REQUEST(0),
    DATA(1),
    REJECT(2);

    private final int value;

    ExtensionMessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
