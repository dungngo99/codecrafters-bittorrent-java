package handler;

import domain.ValueWrapper;
import enums.BEncodeTypeEnum;
import enums.CmdTypeEnum;
import exception.ArgumentException;
import exception.DownloadException;
import exception.ValueWrapperException;
import service.ValueWrapperMap;
import util.DigestUtil;
import util.FileUtil;
import util.PeerUtil;
import util.ValueWrapperUtil;

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
        String outputFilePath = args[1];
        String torrentFilePath = args[2];

        // get .torrent file info from INFO cmd
        CmdHandler infoCmdHandler = CmdStore.getCmd(CmdTypeEnum.INFO.name().toLowerCase());
        ValueWrapper torrentFileVW = infoCmdHandler.getValueWrapper(new String[]{torrentFilePath});

        Map<String, ValueWrapper> downloadVWMap = new HashMap<>() {{
            put(TORRENT_FILE_PATH_KEY, new ValueWrapper(BEncodeTypeEnum.STRING, torrentFilePath));
            put(TORRENT_FILE_VALUE_WRAPPER_KEY, torrentFileVW);
            put(DOWNLOAD_OUTPUT_FILE_PATH_VALUE_WRAPPER_KEY, new ValueWrapper(BEncodeTypeEnum.STRING, outputFilePath));
        }};

        return new ValueWrapper(BEncodeTypeEnum.DICT, downloadVWMap);
    }

    @Override
    public Object handleValueWrapper(ValueWrapper vw) {
        Object o1 = ValueWrapperUtil.convertToObject(vw);
        if (!(o1 instanceof Map<?, ?> downloadMap)) {
            logger.warning("invalid decoded value, throw ex");
            throw new ValueWrapperException("DownloadCmdHandler.getValueWrapper(): invalid decoded value");
        }

        String torrentFilePath = (String) downloadMap.get(TORRENT_FILE_PATH_KEY);
        String outputFilePath = (String) downloadMap.get(DOWNLOAD_OUTPUT_FILE_PATH_VALUE_WRAPPER_KEY);
        Map<?, ?> torrentFileMap = (Map<?, ?>) downloadMap.get(TORRENT_FILE_VALUE_WRAPPER_KEY);
        ValueWrapperMap vwMap = new ValueWrapperMap(torrentFileMap);
        byte[] infoPieces = vwMap.getInfoPieces();
        List<String> pieceHashList = Arrays.stream(DigestUtil.formatPieceHashes(infoPieces)).toList();

        CmdHandler downloadPieceCmdHandler = CmdStore.getCmd(CmdTypeEnum.DOWNLOAD_PIECE.name().toLowerCase());
        try {
            for (int pieceIndex=0; pieceIndex<pieceHashList.size(); pieceIndex++) {
                String pieceOutputFilePath = PeerUtil.formatPieceOutputFilepath(pieceIndex);
                String[] args = new String[]{PIECE_OUTPUT_FILE_OPTION, pieceOutputFilePath, torrentFilePath, String.valueOf(pieceIndex)};

                // download each piece
                ValueWrapper downloadPieceVW = downloadPieceCmdHandler.getValueWrapper(args);
                downloadPieceCmdHandler.handleValueWrapper(downloadPieceVW);
                logger.info(String.format("downloaded piece %s to local file %s", pieceIndex, pieceOutputFilePath));

                // transfer bytes from local files to a single output file
                byte[] pieceOutputFileBytes = FileUtil.readAllBytesFromFile(pieceOutputFilePath);
                FileUtil.writeBytesToFile(outputFilePath, pieceOutputFileBytes, Boolean.TRUE);

                // clean up a local file
                boolean isDelete = FileUtil.deleteFile(pieceOutputFilePath);
                if (isDelete) {
                    logger.info(String.format("deleted local file=%s", pieceOutputFilePath));
                }
            }
        } catch (Exception e) {
            logger.warning(String.format("failed to download pieces to file=%s due to %s", outputFilePath, e.getMessage()));
            throw new DownloadException(e);
        }

        return null;
    }
}
