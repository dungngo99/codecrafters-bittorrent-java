package handler;

import domain.*;
import enums.CmdType;
import enums.TypeEnum;
import exception.ArgumentException;
import exception.MagnetLinkException;
import util.*;

import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

import static constants.Constant.*;

public class MagnetHandshakeCmdHandler implements CmdHandlerV2 {
    private static final Logger logger = Logger.getLogger(MagnetHandshakeCmdHandler.class.getName());

    @Override
    public Object handleCmdHandlerV2(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_MAGNET_HANDSHAKE_CMD) {
            throw new ArgumentException("MagnetHandshakeCmdHandler.getValueWrapper(): invalid params, args=" + Arrays.toString(args));
        }

        // parse magnet link
        CmdHandlerV2 magnetParseCmdHandlerV2 = HybridCmdStore.getCmdHandlerV2(CmdType.MAGNET_PARSE.name().toLowerCase());
        MagnetLinkV1 magnetLinkV1 = (MagnetLinkV1) magnetParseCmdHandlerV2.handleCmdHandlerV2(args);

        // request tracker to get peer list
        PeerRequestQueryParam param = new PeerRequestQueryParam();
        param.setTrackerUrl(magnetLinkV1.getDecodedTr());
        param.setInfoHash(magnetLinkV1.getInfoHash());
        param.setInfoLength(String.valueOf(MAGNET_HANDSHAKE_DEFAULT_INFO_LENGTH));
        param.setPeerId(PeerUtil.getSetPeerId());
        List<PeerInfo> peerInfoList = PeerUtil.performPeerInfoList(param);
        if (peerInfoList.isEmpty()) {
            throw new MagnetLinkException("MagnetHandshakeCmdHandler.getValueWrapper(): no peer found, args=" + Arrays.toString(args));
        }

        // establish the socket connection with 1 peer
        int peerIndex = PeerUtil.randomizePeerBySize(peerInfoList.size());
        PeerInfo peerInfo = peerInfoList.get(peerIndex);
        String ipAddressPortNumber = String.format(IP_ADDRESS_PORT_NUMBER_FORMAT, peerInfo.getIp(), peerInfo.getPort());
        byte[] infoHashBytes = DigestUtil.getBytesFromHex(magnetLinkV1.getInfoHash());
        String clientPeerId = PeerUtil.getSetPeerId();
        Long reservedOption = BitUtil.set(INITIAL_OPTION, TORRENT_METADATA_EXTENSION_OPTION);
        ValueWrapper handshakeVW = ValueWrapperUtil.createHandshakeVW(ipAddressPortNumber, infoHashBytes, clientPeerId, reservedOption);

        // perform the base handshake
        CmdHandler handshakeCmdHandler = HybridCmdStore.getCmdHandler(CmdType.HANDSHAKE.name().toLowerCase());
        Map<String, ValueWrapper> baseHandshakeMap = (Map<String, ValueWrapper>) handshakeCmdHandler.handleValueWrapper(handshakeVW);
        Socket socket = (Socket) baseHandshakeMap.get(HANDSHAKE_PEER_SOCKET_CONNECTION).getO();
        Map<String, ValueWrapper> handshakeMap = new HashMap<>(baseHandshakeMap);

        // listen bitfield peer message
        try {
            PeerMessage bitFieldPeerMessage = PeerUtil.listenBitFieldPeerMessage(socket.getInputStream());
            logger.info("listened for extension handshake bitfield peer message with message=" + bitFieldPeerMessage);
        } catch (Exception e) {
            throw new MagnetLinkException("failed to listen bitfield peer extension message due to error=" + e.getMessage());
        }

        // perform the extension handshake
        try {
            // send the extension handshake
            byte[] peerReservedOptionBytes = (byte[]) baseHandshakeMap.get(HANDSHAKE_PEER_RESERVED_OPTION).getO();
            long peerReservedOption = ByteUtil.getAsLong(peerReservedOptionBytes);
            if (BitUtil.isSet(peerReservedOption, TORRENT_METADATA_EXTENSION_OPTION)) {
                PeerMessage extensionHandshakeMessageRequest = PeerUtil.sendExtensionHandshakeMessage(socket.getOutputStream());
                logger.info("sent extension handshake message: " + extensionHandshakeMessageRequest);
            }

            // receive the extension handshake
            PeerMessage extensionHandshakeMessageResponse = PeerUtil.listenExtensionHandshakeMessage(socket.getInputStream());
            ExtensionHandshakeMessagePayload extensionHandshakeMessagePayload = PeerUtil.parseExtensionHandshakeMessagePayload(extensionHandshakeMessageResponse.getPayload());
            Map<String, Integer> extensionNameToIdMap = extensionHandshakeMessagePayload.getExtensionNameIdMap();
            if (extensionNameToIdMap.containsKey(EXTENSION_HANDSHAKE_UT_METADATA_KEY_NAME)) {
                System.out.println("Peer Metadata Extension ID: " + extensionNameToIdMap.get(EXTENSION_HANDSHAKE_UT_METADATA_KEY_NAME));
            } else {
                logger.warning("peer metadata extension id not found for " + extensionHandshakeMessagePayload);
            }

            Map<String, ValueWrapper> extensionNameToIdVWMap = PeerUtil.convertExtensionNameToIdVWMap(extensionNameToIdMap);
            handshakeMap.putAll(extensionNameToIdVWMap);
        } catch (Exception e) {
            throw new MagnetLinkException("failed to perform extension handshake message due to error=" + e.getMessage());
        }

        return new ValueWrapper(TypeEnum.DICT, handshakeMap);
    }
}
