package handler;

import domain.DownloadParam;
import domain.ValueWrapper;
import enums.CmdType;
import exception.ArgumentException;
import util.DigestUtil;
import util.DownloadUtil;
import util.PeerUtil;
import util.ValueWrapperUtil;

import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static constants.Constant.*;

public class MagnetDownloadPieceCmdHandler implements CmdHandlerV2 {
    private static final Logger logger = Logger.getLogger(MagnetDownloadPieceCmdHandler.class.getName());

    @Override
    public Object handleCmdHandlerV2(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_MAGNET_DOWNLOAD_PIECE_CMD) {
            throw new ArgumentException("MagnetDownloadPieceCmdHandler.handleCmdHandlerV2(): invalid params, args=" + Arrays.toString(args));
        }

        String pieceOutputFilePath = args[DEFAULT_PARAMS_MAGNET_DOWNLOAD_PIECE_FILE_PATH_INDEX];
        String magnetLink = args[DEFAULT_PARAMS_MAGNET_DOWNLOAD_PIECE_MAGNET_LINK_INDEX];
        Integer pieceIndex = Integer.parseInt(args[DEFAULT_PARAMS_MAGNET_DOWNLOAD_PIECE_PIECE_INDEX]);

        // extract metadata information from a peer
        CmdHandlerV2 magnetInfoCmdHandlerV2 = HybridCmdStore.getCmdHandlerV2(CmdType.MAGNET_INFO.name().toLowerCase());
        Map<String, ValueWrapper> extensionMetadataMap = (Map<String, ValueWrapper>) magnetInfoCmdHandlerV2.handleCmdHandlerV2(new String[]{magnetLink});
        Integer length = (Integer) extensionMetadataMap.get(INFO_LENGTH_KEY_INFO_CMD).getO();
        Integer pieceLength = (Integer) extensionMetadataMap.get(INFO_PIECE_LENGTH_INFO_CMD).getO();
        byte[] pieceHashes = (byte[]) extensionMetadataMap.get(INFO_PIECES_INFO_CMD).getO();
        List<String> pieceHashList = Arrays.stream(DigestUtil.formatPieceHashes(pieceHashes)).toList();
        String peerId = (String) extensionMetadataMap.get(HANDSHAKE_PEER_ID).getO();
        Socket socket = (Socket) extensionMetadataMap.get(HANDSHAKE_PEER_SOCKET_CONNECTION).getO();

        // perform base handshake again to get new socket connection
        String infoHash = (String) extensionMetadataMap.get(MAGNET_LINK_INFO_HASH_VALUE_WRAPPER_KEY).getO();
        byte[] infoHashBytes = DigestUtil.getBytesFromHex(infoHash);
        String ipAddress = socket.getInetAddress().getHostAddress();
        Integer portNumber = socket.getPort();
        String ipAddressPortNumber = String.format(IP_ADDRESS_PORT_NUMBER_FORMAT, ipAddress, portNumber);
        String clientPeerId = PeerUtil.getSetPeerId();
        ValueWrapper handshakeVW = ValueWrapperUtil.createHandshakeVW(ipAddressPortNumber, infoHashBytes, clientPeerId);
        CmdHandler handshakeCmdHandler = HybridCmdStore.getCmdHandler(CmdType.HANDSHAKE.name().toLowerCase());
        Map<String, ValueWrapper> peerHandshakeMap = (Map<String, ValueWrapper>) handshakeCmdHandler.handleValueWrapper(handshakeVW);
        Socket newSocket = (Socket) peerHandshakeMap.get(HANDSHAKE_PEER_SOCKET_CONNECTION).getO();

        DownloadParam downloadParam = new DownloadParam();
        downloadParam.setPeerId(peerId);
        downloadParam.setSocket(newSocket);
        downloadParam.setPieceIndex(pieceIndex);
        downloadParam.setInfoPieceLength(pieceLength);
        downloadParam.setInfoLength(length);
        downloadParam.setPieceOutputFilePath(pieceOutputFilePath);
        downloadParam.setPieceHashList(pieceHashList);

        logger.info("downloading piece with params=" + downloadParam);
        return DownloadUtil.downloadPiece(downloadParam);
    }
}
