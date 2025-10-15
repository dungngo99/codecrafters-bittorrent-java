package util;

import domain.ValueWrapper;
import enums.TypeEnum;
import service.BEncoderV2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

import static constants.Constant.*;

public class ValueWrapperUtil {
    private static final Logger logger = Logger.getLogger(ValueWrapperUtil.class.getName());

    public static Object convertToObject(ValueWrapper vw) {
        return convertToObject(vw, Boolean.FALSE);
    }

    public static Object convertToObject(ValueWrapper vw, boolean needConvertString) {
        if (Objects.isNull(vw)) {
            logger.warning("null vw, ignore conversion");
            return null;
        }

        TypeEnum typeEnum = vw.getbEncodeType();
        if (Objects.equals(typeEnum, TypeEnum.INTEGER)) {
            return vw.getO();
        }

        if (Objects.equals(typeEnum, TypeEnum.STRING)) {
            // if use BDecoder, set needConvertString = false
            return needConvertString ? new String((byte[]) vw.getO(), StandardCharsets.UTF_8) : vw.getO();
        }

        if (Objects.equals(typeEnum, TypeEnum.OBJECT)) {
            return vw.getO();
        }

        if (Objects.equals(typeEnum, TypeEnum.LIST)) {
            if (!(vw.getO() instanceof List<?>)) {
                logger.warning("object not BEncodeTypeEnum.LIST, ignore conversion");
                return null;
            }
            List<Object> list = new ArrayList<>();
            for (Object vw_ : (List<?>) vw.getO()) {
                list.add(convertToObject((ValueWrapper) vw_, needConvertString));
            }
            return list;
        }

        if (Objects.equals(typeEnum, TypeEnum.DICT)) {
            if (!(vw.getO() instanceof Map<?, ?>)) {
                logger.warning("object not BEncodeTypeEnum.DICT, ignore conversion");
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
        ValueWrapper infoVW = getObjectFromMap(vw, INFO_KEY_INFO_CMD);
        BEncoderV2 bEncoderV2 = new BEncoderV2(infoVW);
        try {
            byte[] infoBytes = bEncoderV2.encode();
            return DigestUtil.calculateSHA1AsHex(infoBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getInfoHashAsHexFromExtensionMetadata(Map<String, ValueWrapper> metadataExtensionMap) {
        ValueWrapper metadataExtensionMapVW = new ValueWrapper(TypeEnum.DICT, metadataExtensionMap);
        ValueWrapper lengthVW = getObjectFromMap(metadataExtensionMapVW, INFO_LENGTH_KEY_INFO_CMD);
        ValueWrapper nameVW = getObjectFromMap(metadataExtensionMapVW, INFO_NAME_KEY_INFO_CMD);
        ValueWrapper pieceLengthVW = getObjectFromMap(metadataExtensionMapVW, INFO_PIECE_LENGTH_INFO_CMD);
        ValueWrapper pieces = getObjectFromMap(metadataExtensionMapVW, INFO_PIECES_INFO_CMD);
        LinkedHashMap<String, ValueWrapper> infoMap = new LinkedHashMap<>() {{
           put(INFO_LENGTH_KEY_INFO_CMD, lengthVW);
           put(INFO_NAME_KEY_INFO_CMD, nameVW);
           put(INFO_PIECE_LENGTH_INFO_CMD, pieceLengthVW);
           put(INFO_PIECES_INFO_CMD, pieces);
        }};
        ValueWrapper infoMapVW = new ValueWrapper(TypeEnum.DICT, infoMap);
        return getInfoHashAsHex(infoMapVW);
    }

    public static byte[] getInfoHashAsBytes(ValueWrapper vw) {
        ValueWrapper infoVW = getObjectFromMap(vw, INFO_KEY_INFO_CMD);
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

    public static ValueWrapper createHandshakeVW(String ipAddressPortNumber,
                                                 byte[] infoHashBytes,
                                                 String clientPeerId) {
        return createHandshakeVW(ipAddressPortNumber, infoHashBytes, clientPeerId, null);
    }

    public static ValueWrapper createHandshakeVW(String ipAddressPortNumber,
                                                 byte[] infoHashBytes,
                                                 String clientPeerId,
                                                 Long reservedOption) {
        ValueWrapper ipAddressPortNumberVW = new ValueWrapper(TypeEnum.STRING, ipAddressPortNumber);
        ValueWrapper infoHashBytesVW = new ValueWrapper(TypeEnum.OBJECT, infoHashBytes);
        ValueWrapper clientPeerIdVW = new ValueWrapper(TypeEnum.STRING, clientPeerId);
        ValueWrapper reservedOptionVW = new ValueWrapper(TypeEnum.OBJECT, reservedOption);

        Map<String, ValueWrapper> handshakeVWMap = Map.of(
                HANDSHAKE_INFO_HASH_BYTES_VALUE_WRAPPER_KEY, infoHashBytesVW,
                HANDSHAKE_CLIENT_PEER_ID_VALUE_WRAPPER_KEY, clientPeerIdVW,
                HANDSHAKE_IP_PORT_VALUE_WRAPPER_KEY, ipAddressPortNumberVW,
                HANDSHAKE_RESERVED_OPTION_VALUE_WRAPPER_KEY, reservedOptionVW
        );

        return new ValueWrapper(TypeEnum.DICT, handshakeVWMap);
    }
}