package util;

import domain.ValueWrapper;
import enums.BEncodeTypeEnum;
import service.BEncoderV2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

import static constants.Constant.*;
import static constants.Constant.HANDSHAKE_RESERVED_BYTE_LENGTH;

public class ValueWrapperUtil {
    private static final Logger logger = Logger.getLogger(ValueWrapperUtil.class.getName());

    public static Object convertToObject(ValueWrapper vw) {
        return convertToObject(vw, Boolean.FALSE);
    }

    public static Object convertToObject(ValueWrapper vw, boolean needConvertString) {
        if (Objects.isNull(vw)) {
            logger.warning("DecodeHandler: convert, null vw, ignore");
            return null;
        }
        BEncodeTypeEnum typeEnum = vw.getbEncodeType();
        if (Objects.equals(typeEnum, BEncodeTypeEnum.INTEGER)) {
            return vw.getO();
        }
        if (Objects.equals(typeEnum, BEncodeTypeEnum.STRING)) {
            // if use BDecoder, set needConvertString = false
            return needConvertString ? new String((byte[]) vw.getO(), StandardCharsets.UTF_8) : vw.getO();
        }
        if (Objects.equals(typeEnum, BEncodeTypeEnum.LIST)) {
            if (!(vw.getO() instanceof List<?>)) {
                logger.warning("DecodeHandler: convert, object not BEncodeTypeEnum.LIST, ignore");
                return null;
            }
            List<Object> list = new ArrayList<>();
            for (Object vw_ : (List<?>) vw.getO()) {
                list.add(convertToObject((ValueWrapper) vw_, needConvertString));
            }
            return list;
        }
        if (Objects.equals(typeEnum, BEncodeTypeEnum.DICT)) {
            if (!(vw.getO() instanceof Map<?, ?>)) {
                logger.warning("DecodeHandler: convert, object not BEncodeTypeEnum.DICT, ignore");
                return null;
            }
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) vw.getO()).entrySet()) {
                map.put((String) entry.getKey(), convertToObject((ValueWrapper) entry.getValue(), needConvertString));
            }
            return map;
        }
        return null;
    }

    public static ValueWrapper getObjectFromMap(ValueWrapper vw, String key) {
        if (Objects.isNull(vw) || !(vw.getO() instanceof Map<?, ?> map)) {
            return null;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String entryKey = (String) entry.getKey();
            if (Objects.equals(entryKey, key)) {
                return (ValueWrapper) entry.getValue();
            }

            ValueWrapper entryValue = (ValueWrapper) entry.getValue();
            ValueWrapper vw_ = getObjectFromMap(entryValue, key);
            if (Objects.nonNull(vw_)) {
                return vw_;
            }
        }
        return null;
    }

    public static String getInfoHashAsHex(ValueWrapper vw) {
        ValueWrapper infoVW = ValueWrapperUtil.getObjectFromMap(vw, INFO_KEY_INFO_CMD);
        BEncoderV2 bEncoderV2 = new BEncoderV2(infoVW);
        try {
            byte[] infoBytes = bEncoderV2.encode();
            return DigestUtil.calculateSHA1AsHex(infoBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getInfoHashAsBytes(ValueWrapper vw) {
        ValueWrapper infoVW = ValueWrapperUtil.getObjectFromMap(vw, INFO_KEY_INFO_CMD);
        BEncoderV2 bEncoderV2 = new BEncoderV2(infoVW);
        try {
            byte[] infoBytes = bEncoderV2.encode();
            return DigestUtil.calculateSHA1AsBytes(infoBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String urlEncodeInfoHash(String infoHash) {
        StringJoiner joiner = new StringJoiner(PERCENT_SIGN, PERCENT_SIGN, EMPTY_STRING);
        for (int i = 0; i < infoHash.length(); i += 2) {
            joiner.add(infoHash.substring(i, i + 2));
        }
        return joiner.toString();
    }

    public static String getSetPeerId() {
        String peerId = System.getProperty(PEER_ID_KEY);
        if (Objects.nonNull(peerId) && !peerId.isBlank()) {
            return peerId;
        }
        byte[] bytes = new byte[PEER_ID_HEX_LENGTH];
        new Random().nextBytes(bytes);
        peerId = DigestUtil.formatHex(bytes);
        System.setProperty(PEER_ID_KEY, peerId);
        return peerId;
    }

    public static byte[] getHandshakeByteStream(ValueWrapper vw) {
        int length = HANDSHAKE_HEADER_BYTE_LENGTH
                + HANDSHAKE_BITTORRENT_PROTOCOL_STR_LENGTH
                + HANDSHAKE_RESERVED_BYTE_LENGTH
                + HANDSHAKE_INFO_HASH_BYTE_LENGTH
                + HANDSHAKE_PEER_ID_BYTE_LENGTH;
        byte[] handshakeBytes = new byte[length];

        int i = 0;
        handshakeBytes[i++] = HANDSHAKE_BITTORRENT_PROTOCOL_STR_LENGTH.byteValue();

        for (int j=0; j<HANDSHAKE_BITTORRENT_PROTOCOL_STR_LENGTH; j++) {
            handshakeBytes[i++] = (byte) HANDSHAKE_BITTORRENT_PROTOCOL_STR.charAt(j);
        }

        i += HANDSHAKE_RESERVED_BYTE_LENGTH;

        byte[] infoHashBytes = getInfoHashAsBytes(vw);
        assert infoHashBytes.length == HANDSHAKE_INFO_HASH_BYTE_LENGTH;
        for (int j=0; j<HANDSHAKE_INFO_HASH_BYTE_LENGTH; j++) {
            handshakeBytes[i++] = infoHashBytes[j];
        }

        String clientPeerId = getSetPeerId();
        assert clientPeerId.length() == HANDSHAKE_PEER_ID_BYTE_LENGTH;
        for (int j=0; j<HANDSHAKE_PEER_ID_BYTE_LENGTH; j++) {
            handshakeBytes[i++] = (byte) clientPeerId.charAt(j);
        }

        return handshakeBytes;
    }
}