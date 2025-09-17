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

    public ValueWrapper decodeInteger() {
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

    public ValueWrapper decodeString() {
        back();
        ValueWrapper vw = decodeInteger();
        int n = (Integer) vw.getO();
        byte[] bytes = nextNBytes(n);
        return new ValueWrapper(BEncodeTypeEnum.STRING, bytes);
    }

    public char nextChar() {
        return (char) next();
    }

    public byte[] nextNBytes(int n) {
        byte[] bytes = new byte[n];
        for (int i=0; i<n; i++) {
            bytes[i] = next();
        }
        return bytes;
    }

    public byte next() {
        return bytes[i++];
    }

    public byte back() {
        return bytes[i--];
    }

    public int peek() {
        return bytes[i];
    }

    public boolean isEOI(char b) {
        return b == 'e';
    }

    public boolean isColon(char b) {
        return b == ':';
    }

    public boolean isNegative(char b) {
        return b == '-';
    }

    public boolean isEOS() {
        return peek() == -1;
    }
}
