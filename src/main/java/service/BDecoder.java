package service;

import domain.ValueWrapper;
import enums.Type;

import java.util.*;

public class BDecoder {
    private final String str;
    private int i;

    public BDecoder(String str) {
        this.str = str;
        this.i = 0;
    }

    private static boolean isEOI(Character c) {
        return c == 'e';
    }

    private static boolean isColon(Character c) {
        return c == ':';
    }

    private static boolean isNegative(Character c) {
        return c == '-';
    }

    public ValueWrapper decode() {
        if (isEOS()) {
            return null;
        }
        Character indicator = next();
        if (Type.isInteger(indicator)) {
            return decodeInteger();
        }
        if (Type.isList(indicator)) {
            return decodeList();
        }
        if (Type.isDict(indicator)) {
            return decodeDict();
        }
        if (Type.isString(indicator) && !isEOI(indicator) && !isColon(indicator)) {
            return decodeString();
        }
        return null;
    }

    private ValueWrapper decodeInteger() {
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
        return new ValueWrapper(Type.INTEGER, isNegative ? -ans : ans);
    }

    private ValueWrapper decodeString() {
        decrement();
        ValueWrapper vw = decodeInteger();
        int n = ((Long) vw.getO()).intValue();
        String str = new String(nextN(n));
        return new ValueWrapper(Type.STRING, str);
    }

    private ValueWrapper decodeList() {
        List<ValueWrapper> vwList = new ArrayList<>();
        ValueWrapper vw = decode();
        while (Objects.nonNull(vw)) {
            vwList.add(vw);
            vw = decode();
        }
        return new ValueWrapper(Type.LIST, vwList);
    }

    private ValueWrapper decodeDict() {
        Map<String, ValueWrapper> vwMap = new HashMap<>();
        ValueWrapper key = decode();
        while (Objects.nonNull(key)) {
            ValueWrapper value = decode();
            vwMap.put((String) key.getO(), value);
            key = decode();
        }
        return new ValueWrapper(Type.DICT, vwMap);
    }

    private Character next() {
        return str.charAt(increment());
    }

    private char[] nextN(int n) {
        return str.substring(i, incrementByN(n)).toCharArray();
    }

    private void decrement() {
        i--;
    }

    private int increment() {
        return i++;
    }

    private int incrementByN(int n) {
        return i += n;
    }

    private boolean isEOS() {
        return i == str.length();
    }
}
