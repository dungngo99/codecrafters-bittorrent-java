package service;

import domain.ValueWrapper;
import enums.BEncodeTypeEnum;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

public class BEncoderV2 {
    private final ByteArrayInputStream is;
    private int offset;

    public BEncoderV2(ByteArrayInputStream is) {
        this.is = is;
    }

    public ValueWrapper decode() throws IOException {
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
        long ans = 0L;
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

    public ValueWrapper decodeList() throws IOException {
        List<ValueWrapper> vwList = new ArrayList<>();
        ValueWrapper vw = decode();
        while (Objects.nonNull(vw)) {
            vwList.add(vw);
            vw = decode();
        }
        return new ValueWrapper(BEncodeTypeEnum.LIST, vwList);
    }

    public ValueWrapper decodeDict() throws IOException {
        Map<String, ValueWrapper> vwMap = new HashMap<>();
        ValueWrapper key = decode();
        while (Objects.nonNull(key)) {
            ValueWrapper value = decode();
            vwMap.put((String) key.getO(), value);
            key = decode();
        }
        return new ValueWrapper(BEncodeTypeEnum.DICT, vwMap);
    }

    public ValueWrapper decodeString() throws IOException {
        decrement();
        ValueWrapper vw = decodeInteger();
        int n = ((Long) vw.getO()).intValue();
        String str = new String(nextNChar(n));
        return new ValueWrapper(BEncodeTypeEnum.STRING, str);
    }

    public ByteArrayInputStream getIs() {
        return is;
    }

    public char nextChar() {
        return (char) next();
    }

    public char[] nextNChar(int n) throws IOException {
        byte[] bytes = is.readNBytes(n);
        char[] chars = new char[n];
        for (int i=0; i<n; i++) {
            chars[i] = (char) bytes[i];
        }
        return chars;
    }

    public int next() {
        return is.read();
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

    public void decrement() {
        offset--;
    }

    public int increment() {
        return offset++;
    }

    public int incrementByN(int n) {
        return offset+=n;
    }

    public boolean isEOS() {
        return peek() == -1;
    }

    public int peek() {
        is.mark(offset);
        int b = next();
        is.reset();
        return b;
    }
}
