package handler;

import domain.ValueWrapper;
import enums.CmdTypeEnum;
import exception.ArgumentException;
import exception.DownloadException;
import exception.ValueWrapperException;
import service.ValueWrapperMap;
import util.DigestUtil;
import util.FileUtil;
import util.PeerUtil;
import util.ValueWrapperUtil;

import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

import static constants.Constant.*;

public class DownloadCmdHandler implements CmdHandler {
    private static final Logger logger = Logger.getLogger(DownloadCmdHandler.class.getName());

    @Override
    public ValueWrapper getValueWrapper(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_DOWNLOAD_CMD) {
            throw new ArgumentException("DownloadCmdHandler.getValueWrapper(): invalid params, args=" + Arrays.toString(args));
        }

        // connect to 1 peer
        CmdHandler downloadPieceCmdHandler = CmdStore.getCmd(CmdTypeEnum.DOWNLOAD_PIECE.name().toLowerCase());
        return downloadPieceCmdHandler.getValueWrapper(new String[]{args[0], args[1], args[2], EMPTY_STRING});
    }

    @Override
    public Object handleValueWrapper(ValueWrapper vw) {
        Object o1 = ValueWrapperUtil.convertToObject(vw);
        if (!(o1 instanceof Map<?, ?> downloadMap)) {
            logger.warning("invalid decoded value, throw ex");
            throw new ValueWrapperException("DownloadCmdHandler.getValueWrapper(): invalid decoded value");
        }

        Map<?, ?> torrentFileMap = (Map<?, ?>) downloadMap.get(TORRENT_FILE_VALUE_WRAPPER_KEY);
        ValueWrapperMap vwMap = new ValueWrapperMap(torrentFileMap);
        String outputFilePath = (String) downloadMap.get(DOWNLOAD_PIECE_OUTPUT_FILE_PATH_VALUE_WRAPPER_KEY);
        byte[] infoPieces = vwMap.getInfoPieces();
        List<String> pieceHashList = Arrays.stream(DigestUtil.formatPieceHashes(infoPieces)).toList();

        Map<String, Socket> socketMap = (Map<String, Socket>) downloadMap.get(DOWNLOAD_PIECE_VALUE_WRAPPER_KEY);
        String peerId = socketMap.keySet().stream().findFirst().get();
        List<String> pieceOutputFilePaths = new ArrayList<>();

        CmdHandler downloadPieceCmdHandler = CmdStore.getCmd(CmdTypeEnum.DOWNLOAD_PIECE.name().toLowerCase());
        try {
            // download each piece
            for (int pieceIndex=0; pieceIndex<pieceHashList.size(); pieceIndex++) {
                String pieceOutputFilePath = PeerUtil.formatPieceOutputFilepath(peerId, pieceIndex);
                pieceOutputFilePaths.add(pieceOutputFilePath);

                ValueWrapperUtil.modifyVWMap(vw, DOWNLOAD_PIECE_OUTPUT_FILE_PATH_VALUE_WRAPPER_KEY, pieceOutputFilePath);
                ValueWrapperUtil.modifyVWMap(vw, DOWNLOAD_PIECE_INDEX_VALUE_WRAPPER_KEY, pieceIndex);

                downloadPieceCmdHandler.handleValueWrapper(vw);
            }

            // transfer bytes from local files to a single output file
            for (String pieceOutputFilePath: pieceOutputFilePaths) {
                byte[] pieceOutputFileBytes = FileUtil.readAllBytesFromFile(pieceOutputFilePath);
                FileUtil.writeBytesToFile(outputFilePath, pieceOutputFileBytes, Boolean.TRUE);

                // clean up a local file
                boolean isDelete = FileUtil.deleteFile(pieceOutputFilePath);
                if (isDelete) {
                    logger.warning(String.format("deleted local file=%s", pieceOutputFilePath));
                }
            }
        } catch (Exception e) {
            logger.warning(String.format("failed to download pieces from peerId=%s to file=%s due to %s",
                    peerId, outputFilePath, e.getMessage()));
            throw new DownloadException(e);
        }

        return null;
    }
}
