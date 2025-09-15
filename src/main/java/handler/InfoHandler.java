package handler;

import domain.TorrentFileMetadata;
import domain.ValueWrapper;
import exception.ArgumentException;
import service.BDecoder;
import service.BEncoderV2;
import util.FileUtil;
import util.MapUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static constants.Constant.*;

public class InfoHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(InfoHandler.class.getName());

    @Override
    public ValueWrapper getValueWrapper(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_INFO_CMD) {
            throw new ArgumentException("InfoHandler.getValueWrapper(): invalid params, ignore handling: args=" + Arrays.toString(args));
        }
        String path = args[0];
        try {
            File file = FileUtil.getFile(path);
            FileInputStream fileInputStream = new FileInputStream(file.getAbsoluteFile());
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileInputStream.readAllBytes());
            BEncoderV2 bEncoderV2 = new BEncoderV2(byteArrayInputStream);
            return bEncoderV2.decode();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("InfoHandler.getValueWrapper(): failed to cast to FileInputStream, ignore: args=" + Arrays.toString(args), e);
        }
    }

    @Override
    public void handleValueWrapper(ValueWrapper vw) {
        Object o = BDecoder.decode(vw);
        if (!(o instanceof Map<?,?>)) {
            logger.warning("InfoHandler.handleValueWrapper(): invalid decoded value, ignore");
            return;
        }
        Map<?, ?> map = (Map<?, ?>) o;
        TorrentFileMetadata metadata = new TorrentFileMetadata();
        TorrentFileMetadata.Info info = new TorrentFileMetadata.Info();
        String announce = new String(MapUtil.getKey(map, ANNOUNCE_KEY_INFO_CMD, new byte[]{}));
        metadata.setAnnounce(announce);
        String createdBy = new String(MapUtil.getKey(map, CREATED_BY_KEY_INFO_CMD, new byte[]{}));
        metadata.setCreatedBy(createdBy);
        Long length = MapUtil.getNestedKey(map, new String[]{INFO_KEY_INFO_CMD, INFO_LENGTH_KEY_INFO_CMD}, -1L);
        info.setLength(length.intValue());
        String name = new String(MapUtil.getNestedKey(map, new String[]{INFO_KEY_INFO_CMD, INFO_NAME_KEY_INFO_CMD}, new byte[]{}));
        info.setName(name);
        Long pieceLength = MapUtil.getNestedKey(map, new String[]{INFO_KEY_INFO_CMD, INFO_PIECE_LENGTH_INFO_CMD}, -1L);
        info.setPieceLength(pieceLength.intValue());
        byte[] pieces = MapUtil.getNestedKey(map, new String[]{INFO_KEY_INFO_CMD, INFO_PIECES_INFO_CMD}, new byte[]{});
        info.setPieces(pieces);
        metadata.setInfo(info);
        System.out.println(metadata);
    }
}
