import domain.ValueWrapper;
import enums.BEncodeTypeEnum;

import java.util.*;

public class BEncoder {
    private final String str;
    private int i;

    public BEncoder(String str) {
        this.str = str;
        this.i = 0;
    }

    public static boolean isEOI(Character c) {
        return c == 'e';
    }

    public static boolean isColon(Character c) {
        return c == ':';
    }

    public static boolean isNegative(Character c) {
        return c == '-';
    }

    public ValueWrapper decode() {
        if (isEOS()) {
            return null;
        }
        Character indicator = next();
        if (BEncodeTypeEnum.isInteger(indicator)) {
            return decodeInteger();
        }
        if (BEncodeTypeEnum.isList(indicator)) {
            return decodeList();
        }
        if (BEncodeTypeEnum.isDict(indicator)) {
            return decodeDict();
        }
        if (BEncodeTypeEnum.isString(indicator) && !isEOI(indicator) && !isColon(indicator)) {
            return decodeString();
        }
        return null;
    }

    public ValueWrapper decodeInteger() {
        long ans = 0L;
        char c = next();
        boolean isNegative = isNegative(c);
        if (isNegative) {
            c = next();
        }
        while (!isEOI(c) && !isColon(c)) {
            ans = ans * 10;
            ans += c - '0';
            c = next();
        }
        return new ValueWrapper(BEncodeTypeEnum.INTEGER, isNegative ? -ans : ans);
    }

    public ValueWrapper decodeString() {
        decrement();
        ValueWrapper vw = decodeInteger();
        int n = ((Long) vw.getO()).intValue();
        String str = new String(nextN(n));
        return new ValueWrapper(BEncodeTypeEnum.STRING, str);
    }

    public ValueWrapper decodeList() {
        List<ValueWrapper> vwList = new ArrayList<>();
        ValueWrapper vw = decode();
        while (Objects.nonNull(vw)) {
            vwList.add(vw);
            vw = decode();
        }
        return new ValueWrapper(BEncodeTypeEnum.LIST, vwList);
    }

    public ValueWrapper decodeDict() {
        Map<String, ValueWrapper> vwMap = new HashMap<>();
        ValueWrapper key = decode();
        while (Objects.nonNull(key)) {
            ValueWrapper value = decode();
            vwMap.put((String) key.getO(), value);
            key = decode();
        }
        return new ValueWrapper(BEncodeTypeEnum.DICT, vwMap);
    }

    public Character next() {
        return str.charAt(increment());
    }

    public char[] nextN(int n) {
        return str.substring(i, incrementByN(n)).toCharArray();
    }

    public void decrement() {
        i--;
    }

    public int increment() {
        return i++;
    }

    public int incrementByN(int n) {
        return i+=n;
    }

    public boolean isEOS() {
        return i == str.length();
    }
}
