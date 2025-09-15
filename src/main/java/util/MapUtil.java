package util;

import java.util.Arrays;
import java.util.Map;

public class MapUtil {

    public static <T> T getKey(Map<?,?> map, String key, T defaultValue) {
        return map.containsKey(key) ? (T) map.get(key) : defaultValue;
    }

    public static <T> T getNestedKey(Map<?,?> map, String[] keys, T defaultValue) {
        return getNestedKey(map, keys, defaultValue, 0);
    }

    private static <T> T getNestedKey(Map<?,?> map, String[] keys, T defaultValue, int idx) {
        int l = keys.length;
        if (idx < 0 || idx >= l) {
            throw new RuntimeException("MapUtil.getNestedKey(): invalid idx=" + idx + " ; keys=" + Arrays.toString(keys));
        }
        String key = keys[idx];
        if (!map.containsKey(key)) {
            return defaultValue;
        }
        Object o = map.get(key);
        if (idx == l-1) {
            return (T) o;
        }
        if (!(o instanceof Map<?,?>)) {
            return null;
        }
        return getNestedKey((Map<?, ?>) o, keys, defaultValue, idx+1);
    }
}
