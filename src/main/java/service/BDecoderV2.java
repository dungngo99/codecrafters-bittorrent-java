package service;

import domain.ValueWrapper;
import enums.Type;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BDecoderV2 {
    private final byte[] bytes;
    private int i;

    public BDecoderV2(ByteArrayInputStream is) {
        this.bytes = is.readAllBytes();
        this.i = 0;
    }

    public BDecoderV2(byte[] bytes) {
        this(bytes, 0);
    }

    public BDecoderV2(byte[] bytes, int i) {
        this.bytes = bytes;
        this.i = i;
    }

    public int getI() {
        return i;
    }

    public ValueWrapper decode() {
        if (isEmpty() || isEOS()) {
            return null;
        }
        char indicator = nextChar();
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
        char c = nextChar();
        boolean isNegative = isNegative(c);
        if (isNegative) {
            c = nextChar();
        }
        while (!isEOI(c) && !isColon(c)) {
            ans = ans * 10;
            ans += c - '0';
            c = nextChar();
        }
        return new ValueWrapper(Type.INTEGER, isNegative ? -ans : ans);
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
        Map<String, ValueWrapper> vwMap = new LinkedHashMap<>();
        ValueWrapper key = decode();
        while (Objects.nonNull(key)) {
            String key_ = new String((byte[]) key.getO(), StandardCharsets.UTF_8);
            ValueWrapper value = decode();
            vwMap.put(key_, value);
            key = decode();
        }
        return new ValueWrapper(Type.DICT, vwMap);
    }

    private ValueWrapper decodeString() {
        back();
        ValueWrapper vw = decodeInteger();
        int n = (Integer) vw.getO();
        byte[] bytes = nextNBytes(n);
        return new ValueWrapper(Type.STRING, bytes);
    }

    private char nextChar() {
        return (char) next();
    }

    private byte[] nextNBytes(int n) {
        byte[] bytes = new byte[n];
        for (int i = 0; i < n; i++) {
            bytes[i] = next();
        }
        return bytes;
    }

    private byte next() {
        return bytes[i++];
    }

    private byte back() {
        return bytes[i--];
    }

    private int peek() {
        return bytes[i];
    }

    private boolean isEOI(char b) {
        return b == 'e';
    }

    private boolean isColon(char b) {
        return b == ':';
    }

    private boolean isNegative(char b) {
        return b == '-';
    }

    public boolean isEmpty() {
        return bytes.length == 0;
    }

    private boolean isEOS() {
        return peek() == -1;
    }
}
