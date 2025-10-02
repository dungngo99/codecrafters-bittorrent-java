package handler;

import domain.PeerInfo;
import domain.PeerMessage;
import domain.ValueWrapper;
import enums.BEncodeTypeEnum;
import enums.CmdTypeEnum;
import exception.ArgumentException;
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
            throw new ArgumentException("DownloadPieceHandler.getValueWrapper(): invalid params, throw ex: args=" + Arrays.toString(args));
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
            throw new PeerNotExistException("DownloadPieceHandler.getValueWrapper(): no peers exist, throw ex");
        }
        PeerInfo peer = peerList.get(0);
        String ipAddressPortNumber = String.format(IP_ADDRESS_PORT_NUMBER_FORMAT, peer.getIp(), peer.getPort());

        // establish a TCP/IP connection with a peer then perform a handshake
        CmdHandler handshakeCmdHandler = CmdStore.getCmd(CmdTypeEnum.HANDSHAKE.name().toLowerCase());
        ValueWrapper ipAddressPortNumberVW = new ValueWrapper(BEncodeTypeEnum.STRING, ipAddressPortNumber);
        Map<String, ValueWrapper> handshakeVWMap = Map.of(
                TORRENT_FILE_VALUE_WRAPPER_KEY, torrentFileVW,
                HANDSHAKE_IP_PORT_VALUE_WRAPPER_KEY, ipAddressPortNumberVW);
        ValueWrapper handshakeVW = new ValueWrapper(BEncodeTypeEnum.DICT, handshakeVWMap);
        Map<String, Socket> socketMap = (Map<String, Socket>) handshakeCmdHandler.handleValueWrapper(handshakeVW);

        // combine args, .torrent file info, socket info for next stage
        Map<String, ValueWrapper> downloadPieceVWMap = Map.of(
                TORRENT_FILE_VALUE_WRAPPER_KEY, torrentFileVW,
                DOWNLOAD_PIECE_VALUE_WRAPPER_KEY, new ValueWrapper(BEncodeTypeEnum.DICT, socketMap),
                DOWNLOAD_PIECE_OUTPUT_FILE_PATH_VALUE_WRAPPER_KEY, new ValueWrapper(BEncodeTypeEnum.STRING, pieceOutputFilePath),
                DOWNLOAD_PIECE_INDEX_VALUE_WRAPPER_KEY, new ValueWrapper(BEncodeTypeEnum.INTEGER, pieceIndex)
        );
        return new ValueWrapper(BEncodeTypeEnum.DICT, downloadPieceVWMap);
    }

    @Override
    public Object handleValueWrapper(ValueWrapper vw) {
        Object o1 = ValueWrapperUtil.convertToObject(vw);
        if (!(o1 instanceof Map<?, ?> downloadPieceMap)) {
            logger.warning("DownloadPieceCmdHandler.handleValueWrapper(): invalid decoded value, throw ex");
            throw new ValueWrapperException("DownloadPieceCmdHandler.handleValueWrapper(): invalid decoded value");
        }

        Map<?, ?> torrentFileMap = (Map<?, ?>) downloadPieceMap.get(TORRENT_FILE_VALUE_WRAPPER_KEY);
        ValueWrapperMap vwMap = new ValueWrapperMap(torrentFileMap);
        Integer pieceLength = vwMap.getInfoPieceLength();
        byte[] infoPieces = vwMap.getInfoPieces();
        List<String> pieceHashList = Arrays.stream(DigestUtil.formatPieceHashes(infoPieces)).toList();

        Map<String, Socket> socketMap = (Map<String, Socket>) downloadPieceMap.get(DOWNLOAD_PIECE_VALUE_WRAPPER_KEY);
        String outputFilePath = (String) downloadPieceMap.get(DOWNLOAD_PIECE_OUTPUT_FILE_PATH_VALUE_WRAPPER_KEY);
        Integer pieceIndex = (Integer) downloadPieceMap.get(DOWNLOAD_PIECE_INDEX_VALUE_WRAPPER_KEY);
        String peerId = socketMap.keySet().stream().findFirst().get();
        Socket socket = socketMap.values().stream().findFirst().get();

        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            PeerMessage piecesPeerMessage = PeerUtil.listenOwningPiecePeerMessage(is);
            logger.info(String.format("DownloadPieceCmdHandler.handleValueWrapper(): listened for owning pieces=%s from peerId=%s", piecesPeerMessage, peerId));

            PeerMessage interestedPeerMessage = PeerUtil.sendInterestedPeerMessage(os);
            logger.info(String.format("DownloadPieceCmdHandler.handleValueWrapper(): sent interested peer message=%s from peerId=%s", interestedPeerMessage, peerId));

            PeerMessage unchokePeerMessage = PeerUtil.listenUnchokePeerMessage(is);
            logger.info(String.format("DownloadPieceCmdHandler.handleValueWrapper(): listened unchoke peer message=%s from peerId=%s", unchokePeerMessage, peerId));

            int offset = 0;
            while (offset < pieceLength) {
                int length = offset + PEER_MESSAGE_BLOCK_SIZE <= pieceLength ? PEER_MESSAGE_BLOCK_SIZE : pieceLength-offset;
                PeerMessage blockRequestPeerMessage = PeerUtil.sendBlockRequestPeerMessage(os, pieceIndex, offset, length);
                logger.info(String.format("DownloadPieceCmdHandler.handleValueWrapper(): sent block request peer message=%s from peerId=%s, offset=%s", blockRequestPeerMessage, peerId, offset));

                PeerMessage blockResponsePeerMessage = PeerUtil.listenBlockResponsePeerMessage(is);
                logger.fine(String.format("DownloadPieceCmdHandler.handleValueWrapper(): listened block response peer message=%s from peerId=%s, offset=%s", blockResponsePeerMessage, peerId, offset));

                byte[] payload = blockResponsePeerMessage.getPayload();
                FileUtil.writeBytesToFile(outputFilePath, payload, Boolean.TRUE);
                offset += length;
            }

            byte[] bytes = FileUtil.readAllBytesFromFile(outputFilePath);
            String downloadedPieceHash = DigestUtil.calculateSHA1AsHex(bytes);
            if (pieceHashList.contains(downloadedPieceHash)) {
                logger.info("DownloadPieceCmdHandler.handleValueWrapper(): verified the download piece hash against .torrent file piece hash");
            }
        } catch (IOException e) {
            logger.warning(String.format("DownloadPieceCmdHandler.handleValueWrapper(): failed to download a piece from peerId=%s to file=%s; index=%s due to %s",
                    peerId, outputFilePath, pieceIndex, e.getMessage()));
            throw new ValueWrapperException(e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.warning(String.format("HandshakeHandler.handleValueWrapper(): failed to close TCP connection from peerId=%s to file=%s; index=%s due to %s",
                        peerId, outputFilePath, pieceIndex, e.getMessage()));
            }
        }

        return null;
    }
}
