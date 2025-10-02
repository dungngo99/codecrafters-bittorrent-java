package enums;

import java.util.Objects;

public enum BEncodeTypeEnum {
    INTEGER(1, 'i'),
    STRING(2, null),
    LIST(3, 'l'),
    DICT(4, 'd'),
    OBJECT(5, 'o');

    private final int value;
    private final Character indicator;

    BEncodeTypeEnum(int value, Character indicator) {
        this.value = value;
        this.indicator = indicator;
    }

    public static boolean isInteger(Character c) {
        return Objects.equals(c, INTEGER.indicator);
    }

    public static boolean isString(Character c) {
        return !isInteger(c) && !isList(c) && !isDict(c);
    }

    public static boolean isList(Character c) {
        return Objects.equals(c, LIST.indicator);
    }

    public static boolean isDict(Character c) {
        return Objects.equals(c, DICT.indicator);
    }

    public int getValue() {
        return value;
    }

    public Character getIndicator() {
        return indicator;
    }
}
