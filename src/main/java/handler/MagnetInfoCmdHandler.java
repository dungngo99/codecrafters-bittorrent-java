package handler;

import domain.ExtensionMetadataMessagePayload;
import domain.MagnetLinkV1;
import domain.PeerMessage;
import domain.ValueWrapper;
import enums.CmdType;
import exception.ArgumentException;
import exception.MagnetLinkException;
import util.DigestUtil;
import util.PeerUtil;
import util.ValueWrapperUtil;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

import static constants.Constant.*;

public class MagnetInfoCmdHandler implements CmdHandlerV2 {
    private static final Logger logger = Logger.getLogger(MagnetInfoCmdHandler.class.getName());

    @Override
    public Object handleCmdHandlerV2(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_MAGNET_PARSE_CMD) {
            throw new ArgumentException("MagnetInfoCmdHandler.handleCmdHandlerV2(): invalid params, args=" + Arrays.toString(args));
        }

        CmdHandlerV2 cmdHandlerV2 = HybridCmdStore.getCmdHandlerV2(CmdType.MAGNET_HANDSHAKE.name().toLowerCase());
        ValueWrapper vw = (ValueWrapper) cmdHandlerV2.handleCmdHandlerV2(args);
        Map<String, ValueWrapper> handshakeMap = (Map<String, ValueWrapper>) vw.getO();
        Socket socket = (Socket) handshakeMap.get(HANDSHAKE_PEER_SOCKET_CONNECTION).getO();
        Integer peerMetadataExtensionID = (Integer) handshakeMap.get(EXTENSION_HANDSHAKE_UT_METADATA_KEY_NAME).getO();

        Map<String, ValueWrapper> extensionMetadataMap;
        try {
            // send the extension metadata
            OutputStream os = socket.getOutputStream();
            PeerMessage extensionMetadataMessageRequest = PeerUtil.sendExtensionMetadataMessage(os, peerMetadataExtensionID);
            logger.info("sent extension metadata message request " + extensionMetadataMessageRequest);

            // receive the extension metadata
            InputStream is = socket.getInputStream();
            PeerMessage extensionMetadataMessageResponse = PeerUtil.listenExtensionMetadataMessage(is);
            logger.info("received extension metadata message response " + extensionMetadataMessageResponse);
            ExtensionMetadataMessagePayload extensionMetadataMessagePayload = PeerUtil.parseExtensionMetadataMessagePayload(extensionMetadataMessageResponse.getPayload());
            extensionMetadataMap = extensionMetadataMessagePayload.getExtensionMetadataMap();

            // extract, validate, and display info hash
            CmdHandlerV2 magnetParseCmdHandlerV2 = HybridCmdStore.getCmdHandlerV2(CmdType.MAGNET_PARSE.name().toLowerCase());
            MagnetLinkV1 magnetLinkV1 = (MagnetLinkV1) magnetParseCmdHandlerV2.handleCmdHandlerV2(args);
            String infoHash = magnetLinkV1.getInfoHash();
            String metadataExtensionInfoHash = ValueWrapperUtil.getInfoHashAsHexFromExtensionMetadata(extensionMetadataMap);
            if (Objects.equals(infoHash, metadataExtensionInfoHash)) {
                logger.info("magnet link infoHash matched metadata extension infoHash");
            } else {
                logger.warning(String.format("magnet-link infoHash (%s) not matched metadata-extension infoHash (%s)", infoHash, metadataExtensionInfoHash));
            }
            System.out.println("Info Hash: " + infoHash);

            // extract and display other info
            Integer length = (Integer) extensionMetadataMap.get(INFO_LENGTH_KEY_INFO_CMD).getO();
            Integer pieceLength = (Integer) extensionMetadataMap.get(INFO_PIECE_LENGTH_INFO_CMD).getO();
            byte[] pieceHashes = (byte[]) extensionMetadataMap.get(INFO_PIECES_INFO_CMD).getO();
            List<String> pieceHashList = Arrays.stream(DigestUtil.formatPieceHashes(pieceHashes)).toList();

            System.out.println("Length: " + length);
            System.out.println("Piece Length: " + pieceLength);
            System.out.println("Piece Hashes:\n" + String.join("\n", pieceHashList));
        } catch (Exception e) {
            throw new MagnetLinkException("failed to perform extension metadata message due to error=" + e.getMessage());
        }

        return extensionMetadataMap;
    }
}
