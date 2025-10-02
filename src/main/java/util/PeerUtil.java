package util;

import domain.PeerMessage;
import domain.ValueWrapper;
import enums.BEncodeTypeEnum;
import enums.PeerMessageType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static constants.Constant.*;
import static constants.Constant.HANDSHAKE_PEER_ID_BYTE_LENGTH;

public class PeerUtil {

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

        byte[] infoHashBytes = ValueWrapperUtil.getInfoHashAsBytes(vw);
        assert infoHashBytes.length == HANDSHAKE_INFO_HASH_BYTE_LENGTH;
        for (int j=0; j<HANDSHAKE_INFO_HASH_BYTE_LENGTH; j++) {
            handshakeBytes[i++] = infoHashBytes[j];
        }

        String clientPeerId = ValueWrapperUtil.getSetPeerId();
        assert clientPeerId.length() == HANDSHAKE_PEER_ID_BYTE_LENGTH;
        for (int j=0; j<HANDSHAKE_PEER_ID_BYTE_LENGTH; j++) {
            handshakeBytes[i++] = (byte) (clientPeerId.charAt(j) - '0');
        }

        return handshakeBytes;
    }

    public static ValueWrapper decodeHandshake(InputStream is) throws IOException {
        List<ValueWrapper> list = new ArrayList<>();
        ValueWrapper vw = new ValueWrapper(BEncodeTypeEnum.LIST, list);

        list.add(new ValueWrapper(BEncodeTypeEnum.INTEGER, is.read()));

        char[] bitTorrentChars = new char[HANDSHAKE_BITTORRENT_PROTOCOL_STR_LENGTH];
        for (int j=0; j<HANDSHAKE_BITTORRENT_PROTOCOL_STR_LENGTH; j++) {
            bitTorrentChars[j] = (char) is.read();
        }
        list.add(new ValueWrapper(BEncodeTypeEnum.STRING, new String(bitTorrentChars)));

        list.add(new ValueWrapper(BEncodeTypeEnum.STRING, is.readNBytes(HANDSHAKE_RESERVED_BYTE_LENGTH)));

        list.add(new ValueWrapper(BEncodeTypeEnum.STRING, is.readNBytes(HANDSHAKE_INFO_HASH_BYTE_LENGTH)));

        byte[] peerIdBytes = new byte[HANDSHAKE_PEER_ID_BYTE_LENGTH];
        for (int j=0; j<HANDSHAKE_PEER_ID_BYTE_LENGTH; j++) {
            peerIdBytes[j] = (byte) is.read();
        }
        list.add(new ValueWrapper(BEncodeTypeEnum.STRING, DigestUtil.formatHex(peerIdBytes)));

        return vw;
    }

    public static byte[] convertPeerMessageToBytes(PeerMessage peerMessage) {
        int prefixedLength = peerMessage.getPrefixedLength();
        byte messageId = peerMessage.getMessageId();
        byte[] payload = peerMessage.getPayload();
        byte[] bytes = new byte[prefixedLength];
        int i = 0;
        ByteUtil.fill(bytes, ByteUtil.getFromInt(prefixedLength), i+=Integer.BYTES);
        ByteUtil.fill(bytes, ByteUtil.getFromByte(messageId), i+=Byte.BYTES);
        ByteUtil.fill(bytes, payload, i);
        return bytes;
    }

    public static PeerMessage listenOwningPiecePeerMessage(InputStream is) throws IOException {
        PeerMessage peerMessage = new PeerMessage();
        byte[] prefixedLengthBytes = is.readNBytes(PEER_MESSAGE_PREFIX_LENGTH);
        int prefixedLength = ByteUtil.getAsInt(prefixedLengthBytes);
        peerMessage.setPrefixedLength(prefixedLength);

        int messageId = is.read();
        assert messageId == PeerMessageType.OWNING_PIECE.getValue();
        peerMessage.setMessageId((byte) messageId);

        peerMessage.setPayload(is.readNBytes(prefixedLength - PEER_MESSAGE_ID_LENGTH));
        return peerMessage;
    }

    public static PeerMessage sendInterestedPeerMessage(OutputStream os) throws IOException {
        PeerMessage peerMessage = new PeerMessage();
        peerMessage.setPrefixedLength(PEER_MESSAGE_INTERESTED_PREFIX_LENGTH);
        peerMessage.setMessageId((byte) PeerMessageType.INTERESTED.getValue());
        peerMessage.setPayload(new byte[]{});
        SocketUtil.writeThenFlush(os, convertPeerMessageToBytes(peerMessage));
        return peerMessage;
    }

    public static PeerMessage listenUnchokePeerMessage(InputStream is) throws IOException {
        PeerMessage peerMessage = new PeerMessage();
        byte[] prefixedLengthBytes = is.readNBytes(PEER_MESSAGE_PREFIX_LENGTH);
        int prefixedLength = ByteUtil.getAsInt(prefixedLengthBytes);
        peerMessage.setPrefixedLength(prefixedLength);

        int messageId = is.read();
        assert messageId == PeerMessageType.UNCHOKED.getValue();
        peerMessage.setMessageId((byte) messageId);

        peerMessage.setPayload(is.readNBytes(prefixedLength - PEER_MESSAGE_ID_LENGTH));
        return peerMessage;
    }

    public static PeerMessage sendBlockRequestPeerMessage(OutputStream os, int pieceIndex, int offset, int length) throws IOException {
        PeerMessage peerMessage = new PeerMessage();
        peerMessage.setPrefixedLength(PEER_MESSAGE_BLOCK_REQUEST_PREFIX_LENGTH);
        peerMessage.setMessageId((byte) PeerMessageType.BLOCK_REQUEST.getValue());
        byte[] payload = new byte[PEER_MESSAGE_BLOCK_REQUEST_PREFIX_LENGTH - Byte.BYTES];
        int i = 0;
        ByteUtil.fill(payload, ByteUtil.getFromInt(pieceIndex), i+=Integer.BYTES);
        ByteUtil.fill(payload, ByteUtil.getFromInt(offset), i+=Integer.BYTES);
        ByteUtil.fill(payload, ByteUtil.getFromInt(length), i);
        peerMessage.setPayload(payload);
        SocketUtil.writeThenFlush(os, convertPeerMessageToBytes(peerMessage));
        return peerMessage;
    }

    public static PeerMessage listenBlockResponsePeerMessage(InputStream is) throws IOException {
        PeerMessage peerMessage = new PeerMessage();
        byte[] prefixedLengthBytes = is.readNBytes(PEER_MESSAGE_PREFIX_LENGTH);
        int prefixedLength = ByteUtil.getAsInt(prefixedLengthBytes);
        peerMessage.setPrefixedLength(prefixedLength);

        int messageId = is.read();
        assert messageId == PeerMessageType.BLOCK_RESPONSE.getValue();
        peerMessage.setMessageId((byte) messageId);

        peerMessage.setPayload(is.readNBytes(prefixedLength - PEER_MESSAGE_ID_LENGTH));
        return peerMessage;
    }
}
