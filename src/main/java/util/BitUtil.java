package util;

public class BitUtil {

    public static long set(long options, long bit) {
        return options | bit;
    }

    public static long unset(long options, long bit) {
        return options & (~bit);
    }

    public static boolean isSet(long options, long bit) {
        return (options & bit) == bit;
    }
}
