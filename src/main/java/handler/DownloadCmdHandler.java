package handler;

import domain.DownloadJob;
import domain.PeerInfo;
import domain.ValueWrapper;
import enums.TypeEnum;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
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
        CmdHandler infoCmdHandler = HybridCmdStore.getCmdHandler(CmdTypeEnum.INFO.name().toLowerCase());
        ValueWrapper torrentFileVW = infoCmdHandler.getValueWrapper(new String[]{torrentFilePath});
        ValueWrapper torrentFilePathVW = new ValueWrapper(TypeEnum.STRING, torrentFilePath);
        ValueWrapper outputFilePathVW = new ValueWrapper(TypeEnum.STRING, outputFilePath);

        // combine args, .torrent file info for next stage
        Map<String, ValueWrapper> downloadVWMap = Map.of(
                TORRENT_FILE_PATH_KEY, torrentFilePathVW,
                TORRENT_FILE_VALUE_WRAPPER_KEY, torrentFileVW,
                DOWNLOAD_OUTPUT_FILE_PATH_VALUE_WRAPPER_KEY, outputFilePathVW
        );

        return new ValueWrapper(TypeEnum.DICT, downloadVWMap);
    }

    @Override
    public Object handleValueWrapper(ValueWrapper vw) {
        Object o1 = ValueWrapperUtil.convertToObject(vw);
        if (!(o1 instanceof Map<?, ?> downloadMap)) {
            logger.warning("invalid decoded value, throw ex");
            throw new ValueWrapperException("DownloadCmdHandler.getValueWrapper(): invalid decoded value");
        }

        // get info from downloadMap
        String torrentFilePath = (String) downloadMap.get(TORRENT_FILE_PATH_KEY);
        String outputFilePath = (String) downloadMap.get(DOWNLOAD_OUTPUT_FILE_PATH_VALUE_WRAPPER_KEY);
        Map<?, ?> torrentFileMap = (Map<?, ?>) downloadMap.get(TORRENT_FILE_VALUE_WRAPPER_KEY);
        ValueWrapperMap vwMap = new ValueWrapperMap(torrentFileMap);
        byte[] infoPieces = vwMap.getInfoPieces();
        List<String> pieceHashList = Arrays.stream(DigestUtil.formatPieceHashes(infoPieces)).toList();
        int pieceSize = pieceHashList.size();

        // get peer size from downloadMap
        Map<String, ValueWrapper> downloadVWMap = (Map<String, ValueWrapper>) vw.getO();
        ValueWrapper torrentFileVW = downloadVWMap.get(TORRENT_FILE_VALUE_WRAPPER_KEY);
        CmdHandler peersCmdHandler = HybridCmdStore.getCmdHandler(CmdTypeEnum.PEERS.name().toLowerCase());
        List<PeerInfo> peerInfoList = (List<PeerInfo>) peersCmdHandler.handleValueWrapper(torrentFileVW);
        int peerInfoSize = peerInfoList.size();

        CountDownLatch countDownLatch = new CountDownLatch(pieceSize);
        ConcurrentLinkedQueue<DownloadJob> queue = new ConcurrentLinkedQueue<>();

        // spawn download job threads
        for (int peerIndex = 0; peerIndex < peerInfoSize; peerIndex++) {
            Runnable runnable = createDownloadRunnable(queue, countDownLatch, peerIndex);
            String downloadJobThreadName = String.format(DOWNLOAD_JOB_THREAD_NAME, peerIndex);
            new Thread(runnable, downloadJobThreadName).start();
        }

        try {
            // submit a download job for each piece
            List<String> pieceOutputFilePaths = new ArrayList<>();
            for (int pieceIndex = 0; pieceIndex < pieceSize; pieceIndex++) {
                String pieceOutputFilePath = PeerUtil.formatPieceOutputFilepath(pieceIndex);
                pieceOutputFilePaths.add(pieceOutputFilePath);

                DownloadJob downloadJob = new DownloadJob();
                downloadJob.setPieceOutputFileOption(PIECE_OUTPUT_FILE_OPTION);
                downloadJob.setPieceOutputFilePath(pieceOutputFilePath);
                downloadJob.setTorrentFilePath(torrentFilePath);
                downloadJob.setPieceIndex(String.valueOf(pieceIndex));
                downloadJob.setPeerIndex(String.valueOf(pieceIndex % peerInfoSize));
                queue.add(downloadJob);
            }

            countDownLatch.await();

            for (String pieceOutputFilePath: pieceOutputFilePaths) {
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

    private Runnable createDownloadRunnable(ConcurrentLinkedQueue<DownloadJob> queue, CountDownLatch countDownLatch, int peerIndex) {
        return () -> createDownloadRunnable0(queue, countDownLatch, peerIndex);
    }

    private void createDownloadRunnable0(ConcurrentLinkedQueue<DownloadJob> queue, CountDownLatch countDownLatch, int peerIndex) {
        while (true) {
            try {
                boolean shouldExit = createDownloadRunnable1(queue, countDownLatch, peerIndex);
                if (shouldExit) {
                    break;
                }
            } catch (Exception e) {
                logger.warning(String.format("failed to run download job: count=%s due to error %s", countDownLatch.getCount(), e.getMessage()));
            }
        }
    }

    private boolean createDownloadRunnable1(ConcurrentLinkedQueue<DownloadJob> queue, CountDownLatch countDownLatch, int peerIndex) throws InterruptedException {
        // exit thread once no piece left to download
        if (countDownLatch.getCount() == 0) {
            return true;
        }

        // busy-spin if queue is empty
        if (queue.isEmpty()) {
            Thread.sleep(DOWNLOAD_JOB_THREAD_BUSY_SPIN_MS);
            return false;
        }

        DownloadJob downloadJob = queue.poll();
        if (Objects.isNull(downloadJob)) {
            return false;
        }

        // since each thread connects to only one peer
        // if download job is not assigned to the correct peer, pass along
        int downloadJobPeerIndex = Integer.parseInt(downloadJob.getPeerIndex());
        if (peerIndex != downloadJobPeerIndex) {
            queue.add(downloadJob);
            return false;
        }

        // download each piece
        String[] args = downloadJob.convertToArgs();
        CmdHandler downloadPieceCmdHandler = HybridCmdStore.getCmdHandler(CmdTypeEnum.DOWNLOAD_PIECE.name().toLowerCase());
        ValueWrapper downloadPieceVW = downloadPieceCmdHandler.getValueWrapper(args);
        downloadPieceCmdHandler.handleValueWrapper(downloadPieceVW);

        String pieceOutputFilePath = downloadJob.getPieceOutputFilePath();
        String pieceIndex = downloadJob.getPieceIndex();
        logger.info(String.format("downloaded piece %s to local file %s", pieceIndex, pieceOutputFilePath));

        // count down once finished downloading a piece
        countDownLatch.countDown();
        return false;
    }
}
