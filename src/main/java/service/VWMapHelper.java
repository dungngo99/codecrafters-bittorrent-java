package service;

import domain.Peer;
import domain.ValueWrapper;
import util.DigestUtil;
import util.MapUtil;

import java.util.*;

import static constants.Constant.*;

public class VWMapHelper {

    private final Map<?, ?> map;

    public VWMapHelper(Map<?, ?> map) {
        this.map = map;
    }

    public String getAnnounce() {
        return new String(MapUtil.getKey(map, ANNOUNCE_KEY_INFO_CMD, new byte[]{}));
    }

    public String getCreatedBy() {
        return new String(MapUtil.getKey(map, CREATED_BY_KEY_INFO_CMD, new byte[]{}));
    }

    public Integer getInfoLength() {
        return MapUtil.getNestedKey(map, new String[]{INFO_KEY_INFO_CMD, INFO_LENGTH_KEY_INFO_CMD}, -1);
    }

    public String getInfoName() {
        return new String(MapUtil.getNestedKey(map, new String[]{INFO_KEY_INFO_CMD, INFO_NAME_KEY_INFO_CMD}, new byte[]{}));
    }

    public Integer getInfoPieceLength() {
        return MapUtil.getNestedKey(map, new String[]{INFO_KEY_INFO_CMD, INFO_PIECE_LENGTH_INFO_CMD}, -1);
    }

    public byte[] getInfoPieces() {
        return MapUtil.getNestedKey(map, new String[]{INFO_KEY_INFO_CMD, INFO_PIECES_INFO_CMD}, new byte[]{});
    }

    public List<Peer> getPeers() {
        List<Peer> peers = new ArrayList<>();
        ValueWrapper peerVW = MapUtil.getKey(map, PEERS_CMD, null);
        if (Objects.isNull(peerVW)) {
            return peers;
        }

        byte[] bytes = (byte[]) peerVW.getO();
        for (int i = 0; i < bytes.length; i += PEER_BYTE_ARRAY_LENGTH) {
            Peer peer = new Peer();
            peers.add(peer);

            // parse ip address
            StringJoiner ipAddressJoiner = new StringJoiner(DOT_SIGN);
            for (int j = 0; j < PEER_IP_ADDRESS_BYTE_ARRAY_LENGTH; j++) {
                ipAddressJoiner.add(String.valueOf(bytes[i + j] & 0xFF));
            }
            peer.setIp(ipAddressJoiner.toString());

            // parse port number
            byte[] peerPortNumber = new byte[PEER_PORT_NUMBER_BYTE_ARRAY_LENGTH];
            for (int j = 0; j < PEER_PORT_NUMBER_BYTE_ARRAY_LENGTH; j++) {
                peerPortNumber[j] = bytes[i + PEER_IP_ADDRESS_BYTE_ARRAY_LENGTH + j];
            }
            String hex = DigestUtil.formatHex(peerPortNumber);
            peer.setPort(Integer.parseInt(hex, RADIX_HEX_TO_INT));
        }
        return peers;
    }

    public Integer getInterval() {
        return MapUtil.getKey(map, INTERVAL_PEERS_CMD, -1);
    }

    public String getFailureReason() {
        ValueWrapper vw = MapUtil.getKey(map, FAILURE_REASON_PEERS_CMD, null);
        if (Objects.isNull(vw)) {
            return EMPTY_STRING;
        }
        byte[] bytes = (byte[]) vw.getO();
        char[] chars = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            chars[i] = (char) bytes[i];
        }
        return new String(chars);
    }

    public Map<?, ?> getMap() {
        return map;
    }
}
