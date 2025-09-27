package service;

import domain.ValueWrapper;
import enums.BEncodeTypeEnum;

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
        this.bytes = bytes;
        this.i = 0;
    }

    public ValueWrapper decode() {
        if (isEOS()) {
            return null;
        }
        char indicator = nextChar();
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
        return new ValueWrapper(BEncodeTypeEnum.INTEGER, isNegative ? -ans : ans);
    }

    private ValueWrapper decodeList() {
        List<ValueWrapper> vwList = new ArrayList<>();
        ValueWrapper vw = decode();
        while (Objects.nonNull(vw)) {
            vwList.add(vw);
            vw = decode();
        }
        return new ValueWrapper(BEncodeTypeEnum.LIST, vwList);
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
        return new ValueWrapper(BEncodeTypeEnum.DICT, vwMap);
    }

    private ValueWrapper decodeString() {
        back();
        ValueWrapper vw = decodeInteger();
        int n = (Integer) vw.getO();
        byte[] bytes = nextNBytes(n);
        return new ValueWrapper(BEncodeTypeEnum.STRING, bytes);
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

    private boolean isEOS() {
        return peek() == -1;
    }
}
