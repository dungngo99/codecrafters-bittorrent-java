package handler;

import domain.TorrentFile;
import domain.ValueWrapper;
import exception.ArgumentException;
import exception.ValueWrapperException;
import service.BDecoderV2;
import service.ValueWrapperMap;
import util.DigestUtil;
import util.FileUtil;
import util.ValueWrapperUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static constants.Constant.DEFAULT_PARAMS_SIZE_INFO_CMD;

public class InfoCmdHandler implements CmdHandler {
    private static final Logger logger = Logger.getLogger(InfoCmdHandler.class.getName());

    @Override
    public ValueWrapper getValueWrapper(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_INFO_CMD) {
            throw new ArgumentException("InfoCmdHandler.getValueWrapper(): invalid params, ignore handling: args=" + Arrays.toString(args));
        }
        String path = args[0];
        try {
            // read and decode .torrent file locally
            File file = FileUtil.getFile(path);
            FileInputStream fileInputStream = new FileInputStream(file.getAbsoluteFile());
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileInputStream.readAllBytes());
            BDecoderV2 bEncoderV2 = new BDecoderV2(byteArrayInputStream);
            return bEncoderV2.decode();
        } catch (IOException e) {
            throw new RuntimeException("InfoCmdHandler.getValueWrapper(): failed to cast to FileInputStream, ignore: args=" + Arrays.toString(args), e);
        }
    }

    @Override
    public Object handleValueWrapper(ValueWrapper vw) {
        Object o = ValueWrapperUtil.convertToObject(vw);
        if (!(o instanceof Map<?, ?> map
        )) {
            logger.warning("invalid decoded value, throw ex");
            throw new ValueWrapperException("InfoCmdHandler.handleValueWrapper(): invalid decoded value");
        }
        TorrentFile metadata = new TorrentFile();
        TorrentFile.Info info = new TorrentFile.Info();
        metadata.setInfo(info);

        ValueWrapperMap torrentFileHelper = new ValueWrapperMap(map);
        metadata.setAnnounce(torrentFileHelper.getAnnounce());
        metadata.setCreatedBy(torrentFileHelper.getCreatedBy());
        info.setLength(torrentFileHelper.getInfoLength());
        info.setName(torrentFileHelper.getInfoName());
        info.setPieceLength(torrentFileHelper.getInfoPieceLength());
        info.setPieces(torrentFileHelper.getInfoPieces());
        info.setHash(ValueWrapperUtil.getInfoHashAsHex(vw));
        info.setPieceHashes(DigestUtil.formatPieceHashes(info.getPieces()));

        System.out.println(metadata);

        return metadata;
    }
}
