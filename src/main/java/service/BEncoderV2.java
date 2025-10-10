package service;

import domain.ValueWrapper;
import enums.TypeEnum;
import exception.ArgumentException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BEncoderV2 {
    private final ValueWrapper vw;
    private final ByteArrayOutputStream os;

    public BEncoderV2(ValueWrapper vw) {
        this.vw = vw;
        assert vw != null;
        this.os = new ByteArrayOutputStream();
    }

    public byte[] encode() throws IOException {
        encode(vw);
        return os.toByteArray();
    }

    private void encode(ValueWrapper vw_) throws IOException {
        if (Objects.isNull(vw_)) {
            return;
        }
        TypeEnum bEncodeTypeEnum = vw_.getbEncodeType();
        Object o = vw_.getO();
        if (Objects.equals(bEncodeTypeEnum, TypeEnum.INTEGER)) {
            encodeInt((Integer) o);
        } else if (Objects.equals(bEncodeTypeEnum, TypeEnum.STRING)) {
            encodeString((byte[]) o);
        } else if (Objects.equals(bEncodeTypeEnum, TypeEnum.LIST)) {
            encodeList((List<ValueWrapper>) o);
        } else if (Objects.equals(bEncodeTypeEnum, TypeEnum.DICT)) {
            encodeDict((Map<String, ValueWrapper>) o);
        } else {
            throw new ArgumentException("invalid b-encode type from ValueWrapper");
        }
    }

    private void encodeInt(Integer i) throws IOException {
        os.write('i');
        os.write(String.valueOf(i).getBytes(StandardCharsets.UTF_8));
        os.write('e');
    }

    private void encodeString(byte[] bytes) throws IOException {
        os.write(String.valueOf(bytes.length).getBytes(StandardCharsets.UTF_8));
        os.write(':');
        os.write(bytes);
    }

    private void encodeList(List<ValueWrapper> list) throws IOException {
        os.write('l');
        for (ValueWrapper vw : list) {
            encode(vw);
        }
        os.write('e');
    }

    private void encodeDict(Map<String, ValueWrapper> map) throws IOException {
        os.write('d');
        for (Map.Entry<String, ValueWrapper> entry : map.entrySet()) {
            encodeString(entry.getKey().getBytes(StandardCharsets.UTF_8));
            encode(entry.getValue());
        }
        os.write('e');
    }

    public ValueWrapper getVw() {
        return vw;
    }

    public ByteArrayOutputStream getOs() {
        return os;
    }
}
