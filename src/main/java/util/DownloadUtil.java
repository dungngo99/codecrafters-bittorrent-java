package util;

import domain.DownloadParam;
import domain.DownloadResult;
import domain.PeerMessage;
import exception.DownloadPieceException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static constants.Constant.PEER_MESSAGE_BLOCK_SIZE;

public class DownloadUtil {
    private static final Logger logger = Logger.getLogger(DownloadResult.class.getName());

    public static DownloadResult downloadPiece(DownloadParam downloadParam) {
        DownloadResult downloadResult = new DownloadResult();
        Socket socket = downloadParam.getSocket();
        String peerId = downloadParam.getPeerId();
        int pieceIndex = downloadParam.getPieceIndex();
        int infoPieceLength = downloadParam.getInfoPieceLength();
        int infoLength = downloadParam.getInfoLength();
        String pieceOutputFilePath = downloadParam.getPieceOutputFilePath();
        List<String> pieceHashList = downloadParam.getPieceHashList();

        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            PeerMessage bitFieldPeerMessageResponse = PeerUtil.listenBitFieldPeerMessage(is);
            logger.info(String.format("listened for bitfield peer message=%s from peerId=%s", bitFieldPeerMessageResponse, peerId));

            PeerMessage interestedPeerMessageRequest = PeerUtil.sendInterestedPeerMessage(os);
            logger.info(String.format("sent interested peer message=%s from peerId=%s", interestedPeerMessageRequest, peerId));

            PeerMessage unchokePeerMessageResponse = PeerUtil.listenUnchokePeerMessage(is);
            logger.info(String.format("listened unchoke peer message=%s from peerId=%s", unchokePeerMessageResponse, peerId));

            int offset = 0;
            int pieceLength = PeerUtil.calculatePieceLengthByIndex(pieceIndex, infoPieceLength, infoLength);
            while (offset < pieceLength) {
                int length = offset + PEER_MESSAGE_BLOCK_SIZE <= pieceLength ? PEER_MESSAGE_BLOCK_SIZE : pieceLength - offset;
                PeerMessage blockPeerMessageRequest = PeerUtil.sendBlockPeerMessage(os, pieceIndex, offset, length);
                logger.info(String.format("sent request peer message=%s from peerId=%s, offset=%s", blockPeerMessageRequest, peerId, offset));

                PeerMessage piecePeerMessageResponse = PeerUtil.listenPiecePeerMessage(is, pieceIndex, offset, length);
                logger.fine(String.format("listened piece peer message=%s from peerId=%s, offset=%s", piecePeerMessageResponse, peerId, offset));

                byte[] payload = piecePeerMessageResponse.getPayload();
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

        return downloadResult;
    }
}
