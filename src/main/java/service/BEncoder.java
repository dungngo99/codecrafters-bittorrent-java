package service;

import domain.ValueWrapper;
import enums.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BEncoder {
    private final String str;
    private int i;

    public BEncoder(String str) {
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
        int ans = 0;
        char c = next();
        boolean isNegative = isNegative(c);
        if (isNegative) {
            increment();
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
        String str = new String(nextN((int) vw.getO()));
        return new ValueWrapper(Type.STRING, str);
    }

    private ValueWrapper decodeList() {
        List<ValueWrapper> list = new ArrayList<>();
        ValueWrapper vw = decode();
        while (vw != null) {
            list.add(vw);
            vw = decode();
        }
        return new ValueWrapper(Type.LIST, list);
    }

    private ValueWrapper decodeDict() {
        Map<String, ValueWrapper> map = new HashMap<>();
        ValueWrapper key = decode();
        while (key != null) {
            ValueWrapper vw = decode();
            map.put((String) key.getO(), vw);
            key = decode();
        }
        return new ValueWrapper(Type.DICT, map);
    }

    private Character next() {
        return str.charAt(increment());
    }

    private char[] nextN(int n) {
        return str.substring(i, incrementByN(n)).toCharArray();
    }

    private boolean isEOS() {
        return i == str.length();
    }

    private int decrement() {
        return i--;
    }

    private int increment() {
        return i++;
    }

    private int incrementByN(int n) {
        return i += n;
    }

    private int getI() {
        return i;
    }

    private void setI(int i) {
        this.i = i;
    }

    private String getStr() {
        return str;
    }
}
