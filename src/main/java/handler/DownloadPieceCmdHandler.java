package handler;

import domain.PeerInfo;
import domain.PeerMessage;
import domain.ValueWrapper;
import enums.TypeEnum;
import enums.CmdTypeEnum;
import exception.ArgumentException;
import exception.DownloadPieceException;
import exception.PeerNotExistException;
import exception.ValueWrapperException;
import service.ValueWrapperMap;
import util.DigestUtil;
import util.FileUtil;
import util.PeerUtil;
import util.ValueWrapperUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        CmdHandler infoCmdHandler = CmdStore.getCmd(CmdTypeEnum.INFO.name().toLowerCase());
        ValueWrapper torrentFileVW = infoCmdHandler.getValueWrapper(new String[]{torrentFilePath});

        // request tracker to get peer list
        CmdHandler peersCmdHandler = CmdStore.getCmd(CmdTypeEnum.PEERS.name().toLowerCase());
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
        CmdHandler handshakeCmdHandler = CmdStore.getCmd(CmdTypeEnum.HANDSHAKE.name().toLowerCase());
        Map<String, ValueWrapper> socketMap = (Map<String, ValueWrapper>) handshakeCmdHandler.handleValueWrapper(handshakeVW);

        // combine args, .torrent file info, socket info for next stage
        ValueWrapper socketMapVW = new ValueWrapper(TypeEnum.DICT, socketMap);
        ValueWrapper pieceOutputFilePathVW = new ValueWrapper(TypeEnum.STRING, pieceOutputFilePath);
        ValueWrapper pieceIndexVW = new ValueWrapper(TypeEnum.INTEGER, pieceIndex);

        Map<String, ValueWrapper> downloadPieceVWMap = Map.of(
                TORRENT_FILE_VALUE_WRAPPER_KEY, torrentFileVW,
                DOWNLOAD_PIECE_VALUE_WRAPPER_KEY, socketMapVW,
                DOWNLOAD_PIECE_OUTPUT_FILE_PATH_VALUE_WRAPPER_KEY, pieceOutputFilePathVW,
                DOWNLOAD_PIECE_INDEX_VALUE_WRAPPER_KEY, pieceIndexVW
        );

        return new ValueWrapper(TypeEnum.DICT, downloadPieceVWMap);
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

        Map<String, Socket> socketMap = (Map<String, Socket>) downloadPieceMap.get(DOWNLOAD_PIECE_VALUE_WRAPPER_KEY);
        String pieceOutputFilePath = (String) downloadPieceMap.get(DOWNLOAD_PIECE_OUTPUT_FILE_PATH_VALUE_WRAPPER_KEY);
        Integer pieceIndex = (Integer) downloadPieceMap.get(DOWNLOAD_PIECE_INDEX_VALUE_WRAPPER_KEY);
        String peerId = socketMap.keySet().stream().findFirst().get();
        Socket socket = socketMap.values().stream().findFirst().get();

        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            PeerMessage piecesPeerMessage = PeerUtil.listenBitFieldPeerMessage(is);
            logger.info(String.format("listened for bitfield peer message=%s from peerId=%s", piecesPeerMessage, peerId));

            PeerMessage interestedPeerMessage = PeerUtil.sendInterestedPeerMessage(os);
            logger.info(String.format("sent interested peer message=%s from peerId=%s", interestedPeerMessage, peerId));

            PeerMessage unchokePeerMessage = PeerUtil.listenUnchokePeerMessage(is);
            logger.info(String.format("listened unchoke peer message=%s from peerId=%s", unchokePeerMessage, peerId));

            int offset = 0;
            int pieceLength = PeerUtil.calculatePieceLengthByIndex(pieceIndex, infoPieceLength, infoLength);
            while (offset < pieceLength) {
                int length = offset + PEER_MESSAGE_BLOCK_SIZE <= pieceLength ? PEER_MESSAGE_BLOCK_SIZE : pieceLength - offset;
                PeerMessage blockRequestPeerMessage = PeerUtil.sendBlockRequestPeerMessage(os, pieceIndex, offset, length);
                logger.info(String.format("sent request peer message=%s from peerId=%s, offset=%s", blockRequestPeerMessage, peerId, offset));

                PeerMessage piecePeerMessage = PeerUtil.listenPiecePeerMessage(is, pieceIndex, offset, length);
                logger.fine(String.format("listened piece peer message=%s from peerId=%s, offset=%s", piecePeerMessage, peerId, offset));

                byte[] payload = piecePeerMessage.getPayload();
                FileUtil.writeBytesToFile(pieceOutputFilePath, payload, Boolean.TRUE);
                offset += length;
            }

            byte[] bytes = FileUtil.readAllBytesFromFile(pieceOutputFilePath);
            String downloadedPieceHash = DigestUtil.calculateSHA1AsHex(bytes);
            String torrentFilePieceHash = pieceHashList.get(pieceIndex);
            if (Objects.equals(downloadedPieceHash, torrentFilePieceHash)) {
                logger.info("verified the download piece hash against .torrent file piece hash");
            } else {
                logger.warning("not matched the download piece hash against .torrent file piece hash");
            }
        } catch (IOException e) {
            logger.warning(String.format("failed to download a piece from peerId=%s to file=%s; index=%s due to %s",
                    peerId, pieceOutputFilePath, pieceIndex, e.getMessage()));
            throw new DownloadPieceException(e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.warning(String.format("failed to close TCP connection from peerId=%s to file=%s; index=%s due to %s",
                        peerId, pieceOutputFilePath, pieceIndex, e.getMessage()));
            }
        }

        return null;
    }
}
