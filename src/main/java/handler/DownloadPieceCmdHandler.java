package handler;

import domain.DownloadParam;
import domain.PeerInfo;
import domain.ValueWrapper;
import enums.CmdType;
import enums.Type;
import exception.ArgumentException;
import exception.PeerNotExistException;
import exception.ValueWrapperException;
import service.ValueWrapperMap;
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

public class DownloadPieceCmdHandler implements CmdHandler {
    private static final Logger logger = Logger.getLogger(DownloadPieceCmdHandler.class.getName());

    @Override
    public ValueWrapper getValueWrapper(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_DOWNLOAD_PIECE_CMD) {
            throw new ArgumentException("DownloadPieceHandler.getValueWrapper(): invalid params, args=" + Arrays.toString(args));
        }
        String pieceOutputFilePath = args[1];
        String torrentFilePath = args[2];
        Integer pieceIndex = Integer.valueOf(args[3]);

        // get .torrent file info from INFO cmd
        CmdHandler infoCmdHandler = HybridCmdStore.getCmdHandler(CmdType.INFO.name().toLowerCase());
        ValueWrapper torrentFileVW = infoCmdHandler.getValueWrapper(new String[]{torrentFilePath});

        // request tracker to get peer list
        CmdHandler peersCmdHandler = HybridCmdStore.getCmdHandler(CmdType.PEERS.name().toLowerCase());
        List<PeerInfo> peerList = (List<PeerInfo>) peersCmdHandler.handleValueWrapper(torrentFileVW);
        if (Objects.isNull(peerList) || peerList.isEmpty()) {
            throw new PeerNotExistException("DownloadPieceHandler.getValueWrapper(): no peers exist");
        }
        Integer peerIndex;
        if (args.length == PARAMS_SIZE_DOWNLOAD_PIECE_CMD_WITH_PEER_INDEX) {
            peerIndex = Integer.valueOf(args[4]);
        } else {
            peerIndex = PeerUtil.randomizePeerBySize(peerList.size());
        }
        PeerInfo peer = peerList.get(peerIndex);
        String ipAddressPortNumber = String.format(IP_ADDRESS_PORT_NUMBER_FORMAT, peer.getIp(), peer.getPort());

        // establish a TCP/IP connection with a peer then perform a handshake
        byte[] infoHashBytes = ValueWrapperUtil.getInfoHashAsBytes(torrentFileVW);
        String clientPeerId = PeerUtil.getSetPeerId();
        ValueWrapper handshakeVW = ValueWrapperUtil.createHandshakeVW(ipAddressPortNumber, infoHashBytes, clientPeerId);
        CmdHandler handshakeCmdHandler = HybridCmdStore.getCmdHandler(CmdType.HANDSHAKE.name().toLowerCase());
        Map<String, ValueWrapper> peerHandshakeMap = (Map<String, ValueWrapper>) handshakeCmdHandler.handleValueWrapper(handshakeVW);

        // combine args, .torrent file info, socket info for next stage
        ValueWrapper peerHandshakeMapVW = new ValueWrapper(Type.DICT, peerHandshakeMap);
        ValueWrapper pieceOutputFilePathVW = new ValueWrapper(Type.STRING, pieceOutputFilePath);
        ValueWrapper pieceIndexVW = new ValueWrapper(Type.INTEGER, pieceIndex);

        Map<String, ValueWrapper> downloadPieceVWMap = Map.of(
                TORRENT_FILE_VALUE_WRAPPER_KEY, torrentFileVW,
                DOWNLOAD_PIECE_PEER_HANDSHAKE_MAP_VALUE_WRAPPER_KEY, peerHandshakeMapVW,
                DOWNLOAD_PIECE_OUTPUT_FILE_PATH_VALUE_WRAPPER_KEY, pieceOutputFilePathVW,
                DOWNLOAD_PIECE_INDEX_VALUE_WRAPPER_KEY, pieceIndexVW
        );

        return new ValueWrapper(Type.DICT, downloadPieceVWMap);
    }

    @Override
    public Object handleValueWrapper(ValueWrapper vw) {
        Object o1 = ValueWrapperUtil.convertToObject(vw);
        if (!(o1 instanceof Map<?, ?> downloadPieceMap)) {
            logger.warning("invalid decoded value, throw ex");
            throw new ValueWrapperException("DownloadPieceHandler.handleValueWrapper(): invalid decoded value");
        }

        Map<?, ?> torrentFileMap = (Map<?, ?>) downloadPieceMap.get(TORRENT_FILE_VALUE_WRAPPER_KEY);
        ValueWrapperMap vwMap = new ValueWrapperMap(torrentFileMap);
        Integer infoPieceLength = vwMap.getInfoPieceLength();
        Integer infoLength = vwMap.getInfoLength();
        byte[] infoPieces = vwMap.getInfoPieces();
        List<String> pieceHashList = Arrays.stream(DigestUtil.formatPieceHashes(infoPieces)).toList();

        Map<String, Object> peerHandshakeMap = (Map<String, Object>) downloadPieceMap.get(DOWNLOAD_PIECE_PEER_HANDSHAKE_MAP_VALUE_WRAPPER_KEY);
        String pieceOutputFilePath = (String) downloadPieceMap.get(DOWNLOAD_PIECE_OUTPUT_FILE_PATH_VALUE_WRAPPER_KEY);
        Integer pieceIndex = (Integer) downloadPieceMap.get(DOWNLOAD_PIECE_INDEX_VALUE_WRAPPER_KEY);
        String peerId = (String) peerHandshakeMap.get(HANDSHAKE_PEER_ID);
        Socket socket = (Socket) peerHandshakeMap.get(HANDSHAKE_PEER_SOCKET_CONNECTION);

        DownloadParam downloadParam = new DownloadParam();
        downloadParam.setPeerId(peerId);
        downloadParam.setSocket(socket);
        downloadParam.setPieceIndex(pieceIndex);
        downloadParam.setInfoPieceLength(infoPieceLength);
        downloadParam.setInfoLength(infoLength);
        downloadParam.setPieceOutputFilePath(pieceOutputFilePath);
        downloadParam.setPieceHashList(pieceHashList);

        return DownloadUtil.downloadPiece(downloadParam);
    }
}
