package handler;

import domain.PeerInfo;
import domain.PeerRequestQueryParam;
import domain.ValueWrapper;
import enums.CmdType;
import exception.ArgumentException;
import exception.ValueWrapperException;
import service.ValueWrapperMap;
import util.PeerUtil;
import util.ValueWrapperUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static constants.Constant.DEFAULT_PARAMS_SIZE_PEERS_CMD;

public class PeersCmdHandler implements CmdHandler {
    private static final Logger logger = Logger.getLogger(PeersCmdHandler.class.getName());

    @Override
    public ValueWrapper getValueWrapper(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_PEERS_CMD) {
            throw new ArgumentException("PeersCmdHandler.getValueWrapper(): invalid params, ignore handling: args=" + Arrays.toString(args));
        }

        // get .torrent file info from INFO cmd
        CmdHandler infoCmdHandler = HybridCmdStore.getCmdHandler(CmdType.INFO.name().toLowerCase());
        return infoCmdHandler.getValueWrapper(args);
    }

    @Override
    public Object handleValueWrapper(ValueWrapper vw) {
        Object o = ValueWrapperUtil.convertToObject(vw);
        if (!(o instanceof Map<?, ?> map)) {
            logger.warning("invalid decoded value, ignore");
            throw new ValueWrapperException("PeersCmdHandler.handleValueWrapper(): invalid decoded value");
        }

        ValueWrapperMap torrentFileHelper = new ValueWrapperMap(map);
        String trackerUrl = torrentFileHelper.getAnnounce();
        String infoHash = ValueWrapperUtil.getInfoHashAsHex(vw);
        String peerId = PeerUtil.getSetPeerId();
        String infoLength = String.valueOf(torrentFileHelper.getInfoLength());

        PeerRequestQueryParam param = new PeerRequestQueryParam();
        param.setTrackerUrl(trackerUrl);
        param.setInfoHash(infoHash);
        param.setPeerId(peerId);
        param.setInfoLength(infoLength);

        List<PeerInfo> peerInfoList = PeerUtil.performPeerInfoList(param);
        peerInfoList.forEach(System.out::println);

        return peerInfoList;
    }
}
