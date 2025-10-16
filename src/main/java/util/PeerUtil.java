package util;

import domain.*;
import enums.ExtensionMessageType;
import enums.Type;
import enums.PeerMessageType;
import service.BDecoderV2;
import service.BEncoderV2;
import service.HttpClient;
import service.ValueWrapperMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Logger;

import static constants.Constant.*;
import static constants.Constant.HANDSHAKE_PEER_ID_BYTE_LENGTH;

public class PeerUtil {
    private static final Logger logger = Logger.getLogger(PeerUtil.class.getName());

    public static byte[] getHandshakeByteStream(String clientPeerId, byte[] infoHashBytes, Long reservedOption) {
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

        if (Objects.isNull(reservedOption)) {
            i += HANDSHAKE_RESERVED_BYTE_LENGTH;
        } else {
            byte[] longBytes = ByteUtil.getFromLong(reservedOption);
            for (int j=0; j<HANDSHAKE_RESERVED_BYTE_LENGTH; j++) {
                handshakeBytes[i++] = longBytes[j];
            }
        }

        assert infoHashBytes.length == HANDSHAKE_INFO_HASH_BYTE_LENGTH;
        for (int j=0; j<HANDSHAKE_INFO_HASH_BYTE_LENGTH; j++) {
            handshakeBytes[i++] = infoHashBytes[j];
        }

        assert clientPeerId.length() == HANDSHAKE_PEER_ID_BYTE_LENGTH;
        for (int j=0; j<HANDSHAKE_PEER_ID_BYTE_LENGTH; j++) {
            handshakeBytes[i++] = (byte) (clientPeerId.charAt(j) - '0');
        }

        return handshakeBytes;
    }

    public static ValueWrapper decodeHandshake(InputStream is) throws IOException {
        List<ValueWrapper> list = new ArrayList<>();
        ValueWrapper vw = new ValueWrapper(Type.LIST, list);

        list.add(new ValueWrapper(Type.INTEGER, is.read()));

        char[] bitTorrentChars = new char[HANDSHAKE_BITTORRENT_PROTOCOL_STR_LENGTH];
        for (int j=0; j<HANDSHAKE_BITTORRENT_PROTOCOL_STR_LENGTH; j++) {
            bitTorrentChars[j] = (char) is.read();
        }
        list.add(new ValueWrapper(Type.STRING, new String(bitTorrentChars)));

        list.add(new ValueWrapper(Type.OBJECT, is.readNBytes(HANDSHAKE_RESERVED_BYTE_LENGTH)));

        list.add(new ValueWrapper(Type.OBJECT, is.readNBytes(HANDSHAKE_INFO_HASH_BYTE_LENGTH)));

        byte[] peerIdBytes = new byte[HANDSHAKE_PEER_ID_BYTE_LENGTH];
        for (int j=0; j<HANDSHAKE_PEER_ID_BYTE_LENGTH; j++) {
            peerIdBytes[j] = (byte) is.read();
        }
        list.add(new ValueWrapper(Type.STRING, DigestUtil.formatHex(peerIdBytes)));

        return vw;
    }

    public static byte[] convertPeerMessageToBytes(PeerMessage peerMessage) {
        int prefixedLength = peerMessage.getPrefixedLength();
        byte messageId = peerMessage.getMessageId();
        byte[] payload = peerMessage.getPayload();
        byte[] bytes = new byte[PEER_MESSAGE_PREFIXED_LENGTH + prefixedLength];
        int i = 0;
        ByteUtil.fill(bytes, ByteUtil.getFromInt(prefixedLength), i);
        i += Integer.BYTES;
        ByteUtil.fill(bytes, ByteUtil.getFromByte(messageId), i);
        i += Byte.BYTES;
        ByteUtil.fill(bytes, payload, i);
        return bytes;
    }

    public static PeerMessage listenBitFieldPeerMessage(InputStream is) throws IOException {
        PeerMessage peerMessage = new PeerMessage();
        byte[] prefixedLengthBytes = is.readNBytes(PEER_MESSAGE_PREFIXED_LENGTH);
        int prefixedLength = ByteUtil.getAsInt(prefixedLengthBytes);
        peerMessage.setPrefixedLength(prefixedLength);

        int messageId = is.read();
        assert messageId == PeerMessageType.BITFIELD.getValue();
        peerMessage.setMessageId((byte) messageId);

        peerMessage.setPayload(is.readNBytes(prefixedLength - PEER_MESSAGE_ID_LENGTH));
        return peerMessage;
    }

    public static PeerMessage sendInterestedPeerMessage(OutputStream os) throws IOException {
        PeerMessage peerMessage = new PeerMessage();
        peerMessage.setPrefixedLength(PEER_MESSAGE_INTERESTED_PREFIXED_LENGTH);
        peerMessage.setMessageId((byte) PeerMessageType.INTERESTED.getValue());
        peerMessage.setPayload(new byte[]{});
        SocketUtil.writeThenFlush(os, convertPeerMessageToBytes(peerMessage));
        return peerMessage;
    }

    public static PeerMessage listenUnchokePeerMessage(InputStream is) throws IOException {
        PeerMessage peerMessage = new PeerMessage();
        byte[] prefixedLengthBytes = is.readNBytes(PEER_MESSAGE_PREFIXED_LENGTH);
        int prefixedLength = ByteUtil.getAsInt(prefixedLengthBytes);
        peerMessage.setPrefixedLength(prefixedLength);

        int messageId = is.read();
        assert messageId == PeerMessageType.UNCHOKE.getValue();
        peerMessage.setMessageId((byte) messageId);

        peerMessage.setPayload(is.readNBytes(prefixedLength - PEER_MESSAGE_ID_LENGTH));
        return peerMessage;
    }

    public static PeerMessage sendBlockPeerMessage(OutputStream os, int pieceIndex, int offset, int length) throws IOException {
        PeerMessage peerMessage = new PeerMessage();
        peerMessage.setPrefixedLength(PEER_MESSAGE_REQUEST_PREFIXED_LENGTH);
        peerMessage.setMessageId((byte) PeerMessageType.REQUEST.getValue());
        byte[] payload = new byte[PEER_MESSAGE_REQUEST_PREFIXED_LENGTH - Byte.BYTES];
        int i = 0;
        ByteUtil.fill(payload, ByteUtil.getFromInt(pieceIndex), i);
        i += Integer.BYTES;
        ByteUtil.fill(payload, ByteUtil.getFromInt(offset), i);
        i += Integer.BYTES;
        ByteUtil.fill(payload, ByteUtil.getFromInt(length), i);
        peerMessage.setPayload(payload);
        SocketUtil.writeThenFlush(os, convertPeerMessageToBytes(peerMessage));
        return peerMessage;
    }

    public static PeerMessage listenPiecePeerMessage(InputStream is, int pieceIndex, int offset, int pieceLength) throws IOException {
        PeerMessage peerMessage = new PeerMessage();
        byte[] prefixedLengthBytes = is.readNBytes(PEER_MESSAGE_PREFIXED_LENGTH);
        int prefixedLength = ByteUtil.getAsInt(prefixedLengthBytes);
        peerMessage.setPrefixedLength(prefixedLength);

        int messageId = is.read();
        assert messageId == PeerMessageType.PIECE.getValue();
        peerMessage.setMessageId((byte) messageId);

        byte[] pieceIndexBytes = is.readNBytes(Integer.BYTES);
        int pieceIndex_ = ByteUtil.getAsInt(pieceIndexBytes);
        assert pieceIndex_ == pieceIndex;

        byte[] offsetBytes = is.readNBytes(Integer.BYTES);
        int offset_ = ByteUtil.getAsInt(offsetBytes);
        assert offset_ == offset;

        int pieceLength_ = prefixedLength - PEER_MESSAGE_ID_LENGTH - Integer.BYTES - Integer.BYTES;
        assert pieceLength_ == pieceLength;
        peerMessage.setPayload(is.readNBytes(pieceLength));
        return peerMessage;
    }

    public static int calculatePieceLengthByIndex(int pieceIndex, int infoPieceLength, int infoLength) {
        int maxPieceIndex = (int) (Math.ceil(infoLength * 1.0 / infoPieceLength) -1);
        return maxPieceIndex == pieceIndex
                ? infoLength - infoPieceLength * maxPieceIndex
                : infoPieceLength;
    }

    public static String formatPieceOutputFilepath(Integer pieceIndex) {
        return String.format(PIECE_OUTPUT_FILE_PATH_FORMAT, pieceIndex);
    }

    public static Integer randomizePeerBySize(Integer peerSize) {
        Random random = new Random();
        return random.nextInt(peerSize);
    }

    public static String getSetPeerId() {
        String peerId = System.getProperty(PEER_ID_KEY);
        if (Objects.nonNull(peerId) && !peerId.isBlank()) {
            return peerId;
        }
        Random random = new Random(System.currentTimeMillis());
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<PEER_ID_LENGTH; i++) {
            sb.append(random.nextInt(0, 10));
        }
        peerId = sb.toString();
        System.setProperty(PEER_ID_KEY, peerId);
        return peerId;
    }

    public static List<PeerInfo> performPeerInfoList(PeerRequestQueryParam param) {
        String trackerUrl = param.getTrackerUrl();
        String infoHash = param.getInfoHash();
        String infoLength = param.getInfoLength();
        String peerId = param.getPeerId();

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(INFO_HASH_QUERY_PARAM_KEY, ValueWrapperUtil.urlEncodeInfoHash(infoHash));
        queryParams.put(PEER_ID_QUERY_PARAM_KEY, peerId);
        queryParams.put(PORT_QUERY_PARAM_KEY, DEFAULT_PORT_QUERY_PARAM_VALUE);
        queryParams.put(UPLOADED_QUERY_PARAM_KEY, String.valueOf(DEFAULT_UPLOADED_QUERY_PARAM_VALUE));
        queryParams.put(DOWNLOADED_QUERY_PARAM_KEY, String.valueOf(DEFAULT_DOWNLOADED_QUERY_PARAM_VALUE));
        queryParams.put(LEFT_QUERY_PARAM_KEY, infoLength);
        queryParams.put(COMPACT_QUERY_PARAM_KEY, String.valueOf(DEFAULT_COMPACT_QUERY_PARAM_VALUE));

        List<PeerInfo> peerList = new ArrayList<>();
        HttpRequestOption option = new HttpRequestOption.Builder().ofNeedUrlEncodeQueryParam(Boolean.FALSE).build();
        HttpResponse httpResponse = HttpClient.DEFAULT_HTTP_CLIENT.get(trackerUrl, Map.of(), queryParams, option);
        if (Objects.isNull(httpResponse) || !HttpUtil.isSuccessHttpRequest(httpResponse.getStatus())) {
            logger.warning("failed to call tracker server, status code=" + httpResponse.getStatus());
            return peerList;
        }

        BDecoderV2 bDecoderV2 = new BDecoderV2(httpResponse.getBytes());
        ValueWrapper trackerVW = bDecoderV2.decode();
        if (Objects.isNull(trackerVW)
                || !Objects.equals(trackerVW.getbEncodeType(), Type.DICT)
                || !(trackerVW.getO() instanceof Map<?, ?> trackerVWMap)) {
            logger.warning("invalid tracker value wrapper, ignore parsing");
            return peerList;
        }

        ValueWrapperMap valueWrapperHelper = new ValueWrapperMap(trackerVWMap);
        if (!valueWrapperHelper.getFailureReason().isBlank()) {
            System.out.println(valueWrapperHelper.getFailureReason());
            return peerList;
        }

        peerList.addAll(valueWrapperHelper.getPeers());
        return peerList;
    }

    public static PeerMessage sendExtensionHandshakeMessage(OutputStream os) throws IOException {
        PeerMessage peerMessage = new PeerMessage();
        peerMessage.setMessageId(DEFAULT_EXTENSION_HANDSHAKE_MESSAGE_ID.byteValue());

        Map<String, ValueWrapper> extensionHandshakeMessageMap = new HashMap<>();
        ValueWrapper extensionMessageMapVW = new ValueWrapper(Type.DICT, extensionHandshakeMessageMap);

        Map<String, ValueWrapper> subExtensionHandshakeMessageMap = new HashMap<>();
        ValueWrapper subExtensionHandshakeMessageMapVW = new ValueWrapper(Type.DICT, subExtensionHandshakeMessageMap);
        extensionHandshakeMessageMap.put(EXTENSION_HANDSHAKE_M_KEY_NAME, subExtensionHandshakeMessageMapVW);

        ValueWrapper extensionHandshakeMessageUtMetadataVW = new ValueWrapper(Type.INTEGER, DEFAULT_EXTENSION_HANDSHAKE_UT_METADATA_ID);
        subExtensionHandshakeMessageMap.put(EXTENSION_HANDSHAKE_UT_METADATA_KEY_NAME, extensionHandshakeMessageUtMetadataVW);

        BEncoderV2 bEncoderV2 = new BEncoderV2(extensionMessageMapVW);
        byte[] extensionMessageBytes = bEncoderV2.encode();
        byte[] payload = new byte[DEFAULT_EXTENSION_HANDSHAKE_MESSAGE_ID_LENGTH + extensionMessageBytes.length];
        int offset = 0;
        ByteUtil.fill(payload, new byte[]{DEFAULT_EXTENSION_HANDSHAKE_EXTENSION_MESSAGE_ID.byteValue()}, offset);
        offset++;
        ByteUtil.fill(payload, extensionMessageBytes, offset);
        peerMessage.setPayload(payload);

        int prefixedLength = PEER_MESSAGE_ID_LENGTH + payload.length;
        peerMessage.setPrefixedLength(prefixedLength);

        byte[] bytes = convertPeerMessageToBytes(peerMessage);
        SocketUtil.writeThenFlush(os, bytes);

        return peerMessage;
    }

    public static PeerMessage listenExtensionHandshakeMessage(InputStream is) throws IOException {
        PeerMessage peerMessage = new PeerMessage();
        byte[] prefixedLengthBytes = is.readNBytes(PEER_MESSAGE_PREFIXED_LENGTH);
        int prefixedLength = ByteUtil.getAsInt(prefixedLengthBytes);
        peerMessage.setPrefixedLength(prefixedLength);

        int messageId = is.read();
        assert messageId == DEFAULT_EXTENSION_HANDSHAKE_MESSAGE_ID;
        peerMessage.setMessageId((byte) messageId);

        peerMessage.setPayload(is.readNBytes(prefixedLength - PEER_MESSAGE_ID_LENGTH));
        return peerMessage;
    }

    public static ExtensionHandshakeMessagePayload parseExtensionHandshakeMessagePayload(byte[] bytes) {
        ExtensionHandshakeMessagePayload peerExtensionMessage = new ExtensionHandshakeMessagePayload();
        int offset = 0;
        peerExtensionMessage.setPeerMessageId(bytes[offset]);
        offset++;

        Map<String, Integer> extensionNameIdMap = new HashMap<>();
        peerExtensionMessage.setExtensionNameIdMap(extensionNameIdMap);

        byte[] payload = Arrays.copyOfRange(bytes, offset, bytes.length);
        BDecoderV2 bDecoderV2 = new BDecoderV2(payload);
        ValueWrapper valueWrapper = bDecoderV2.decode();
        assert Objects.equals(valueWrapper.getbEncodeType(), Type.DICT);
        Map<String, ValueWrapper> map = (Map<String, ValueWrapper>) valueWrapper.getO();

        ValueWrapper mMapVW = map.get(EXTENSION_HANDSHAKE_M_KEY_NAME);
        if (Objects.isNull(mMapVW)) {
            return peerExtensionMessage;
        }
        assert Objects.equals(mMapVW.getbEncodeType(), Type.DICT);
        Map<String, ValueWrapper> mMap = (Map<String, ValueWrapper>) mMapVW.getO();
        for (Map.Entry<String, ValueWrapper> mMapEntry: mMap.entrySet()) {
            String key = mMapEntry.getKey();
            ValueWrapper valueVW = mMapEntry.getValue();
            if (!Type.isInteger(valueVW.getbEncodeType())) {
                continue;
            }
            Integer value = (Integer) valueVW.getO();
            extensionNameIdMap.put(key, value);
        }

        return peerExtensionMessage;
    }

    public static Map<String, ValueWrapper> convertExtensionNameToIdVWMap(Map<String, Integer> extensionIdToNameMap) {
        Map<String, ValueWrapper> extensionIdNameVWMap = new HashMap<>();
        if (extensionIdToNameMap.containsKey(EXTENSION_HANDSHAKE_UT_METADATA_KEY_NAME)) {
            Integer extensionId = extensionIdToNameMap.get(EXTENSION_HANDSHAKE_UT_METADATA_KEY_NAME);
            extensionIdNameVWMap.put(EXTENSION_HANDSHAKE_UT_METADATA_KEY_NAME, new ValueWrapper(Type.INTEGER, extensionId));
        }
        return extensionIdNameVWMap;
    }

    public static PeerMessage sendExtensionMetadataMessage(OutputStream os, Integer peerMetadataExtensionID) throws IOException {
        PeerMessage peerMessage = new PeerMessage();
        peerMessage.setMessageId(DEFAULT_EXTENSION_HANDSHAKE_MESSAGE_ID.byteValue());

        Map<String, ValueWrapper> extensionMetadataMessageMap = new HashMap<>();
        ValueWrapper extensionMetadataMessageMapVW = new ValueWrapper(Type.DICT, extensionMetadataMessageMap);

        ValueWrapper msgTypeVW = new ValueWrapper(Type.INTEGER, ExtensionMessageType.REQUEST.getValue());
        ValueWrapper pieceVW = new ValueWrapper(Type.INTEGER, DEFAULT_EXTENSION_METADATA_PIECE_ID);
        extensionMetadataMessageMap.put(EXTENSION_METADATA_MSG_TYPE_KEY_NAME, msgTypeVW);
        extensionMetadataMessageMap.put(EXTENSION_METADATA_PIECE_KEY_NAME, pieceVW);

        BEncoderV2 bEncoderV2 = new BEncoderV2(extensionMetadataMessageMapVW);
        byte[] extensionMetadataMessageMapVWBytes = bEncoderV2.encode();
        byte[] payload = new byte[DEFAULT_EXTENSION_METADATA_MESSAGE_ID_LENGTH + extensionMetadataMessageMapVWBytes.length];
        int offset = 0;
        ByteUtil.fill(payload, new byte[]{peerMetadataExtensionID.byteValue()}, offset);
        offset++;
        ByteUtil.fill(payload, extensionMetadataMessageMapVWBytes, offset);
        peerMessage.setPayload(payload);

        int prefixedLength = PEER_MESSAGE_ID_LENGTH + payload.length;
        peerMessage.setPrefixedLength(prefixedLength);

        SocketUtil.writeThenFlush(os, convertPeerMessageToBytes(peerMessage));
        return peerMessage;
    }

    public static PeerMessage listenExtensionMetadataMessage(InputStream is) throws IOException {
        PeerMessage peerMessage = new PeerMessage();
        byte[] prefixedLengthBytes = is.readNBytes(PEER_MESSAGE_PREFIXED_LENGTH);
        int prefixedLength = ByteUtil.getAsInt(prefixedLengthBytes);
        peerMessage.setPrefixedLength(prefixedLength);

        int messageId = is.read();
        assert messageId == DEFAULT_EXTENSION_HANDSHAKE_MESSAGE_ID;
        peerMessage.setMessageId((byte) messageId);

        peerMessage.setPayload(is.readNBytes(prefixedLength - PEER_MESSAGE_ID_LENGTH));
        return peerMessage;
    }

    public static ExtensionMetadataMessagePayload parseExtensionMetadataMessagePayload(byte[] bytes) {
        ExtensionMetadataMessagePayload payload = new ExtensionMetadataMessagePayload();
        int offset = 0;
        payload.setPeerMessageId(bytes[offset]);
        offset++;

        Map<String, ValueWrapper> extensionMetadataMap = new HashMap<>();
        payload.setExtensionMetadataMap(extensionMetadataMap);

        BDecoderV2 metadataBDecoderV2 = new BDecoderV2(bytes, offset);
        ValueWrapper metadataVW = metadataBDecoderV2.decode();
        Map<String, ValueWrapper> metadataVWMap = (Map<String, ValueWrapper>) metadataVW.getO();
        extensionMetadataMap.putAll(metadataVWMap);

        Integer msgType = (Integer) extensionMetadataMap.get(EXTENSION_METADATA_MSG_TYPE_KEY_NAME).getO();
        assert Objects.equals(msgType, ExtensionMessageType.DATA.getValue());

        offset = metadataBDecoderV2.getI();

        BDecoderV2 metadataPieceBDecoderV2 = new BDecoderV2(bytes, offset);
        ValueWrapper metadataPieceVW = metadataPieceBDecoderV2.decode();
        Map<String, ValueWrapper> metadataPieceVWMap = (Map<String, ValueWrapper>) metadataPieceVW.getO();
        extensionMetadataMap.putAll(metadataPieceVWMap);

        return payload;
    }
}
