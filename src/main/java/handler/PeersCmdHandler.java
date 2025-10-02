package handler;

import domain.HttpRequestOption;
import domain.HttpResponse;
import domain.PeerInfo;
import domain.ValueWrapper;
import enums.BEncodeTypeEnum;
import enums.CmdTypeEnum;
import exception.ArgumentException;
import exception.ValueWrapperException;
import service.BDecoderV2;
import service.HttpClient;
import service.ValueWrapperMap;
import util.HttpUtil;
import util.ValueWrapperUtil;

import java.util.*;
import java.util.logging.Logger;

import static constants.Constant.*;

public class PeersCmdHandler implements CmdHandler {
    private static final Logger logger = Logger.getLogger(PeersCmdHandler.class.getName());

    @Override
    public ValueWrapper getValueWrapper(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_PEERS_CMD) {
            throw new ArgumentException("PeersCmdHandler.getValueWrapper(): invalid params, ignore handling: args=" + Arrays.toString(args));
        }

        // get .torrent file info from INFO cmd
        CmdHandler infoCmdHandler = CmdStore.getCmd(CmdTypeEnum.INFO.name().toLowerCase());
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
        Map<String, String> queryParams = new HashMap<>();
        String infoHash = ValueWrapperUtil.getInfoHashAsHex(vw);
        queryParams.put(INFO_HASH_QUERY_PARAM_KEY, ValueWrapperUtil.urlEncodeInfoHash(infoHash));
        queryParams.put(PEER_ID_QUERY_PARAM_KEY, ValueWrapperUtil.getSetPeerId());
        queryParams.put(PORT_QUERY_PARAM_KEY, DEFAULT_PORT_QUERY_PARAM_VALUE);
        queryParams.put(UPLOADED_QUERY_PARAM_KEY, String.valueOf(DEFAULT_UPLOADED_QUERY_PARAM_VALUE));
        queryParams.put(DOWNLOADED_QUERY_PARAM_KEY, String.valueOf(DEFAULT_DOWNLOADED_QUERY_PARAM_VALUE));
        queryParams.put(LEFT_QUERY_PARAM_KEY, String.valueOf(torrentFileHelper.getInfoLength()));
        queryParams.put(COMPACT_QUERY_PARAM_KEY, String.valueOf(DEFAULT_COMPACT_QUERY_PARAM_VALUE));

        List<PeerInfo> peerList = new ArrayList<>();
        String trackerUrl = torrentFileHelper.getAnnounce();
        HttpRequestOption option = new HttpRequestOption.Builder().ofNeedUrlEncodeQueryParam(Boolean.FALSE).build();
        HttpResponse httpResponse = HttpClient.DEFAULT_HTTP_CLIENT.get(trackerUrl, Map.of(), queryParams, option);
        if (Objects.isNull(httpResponse) || !HttpUtil.isSuccessHttpRequest(httpResponse.getStatus())) {
            logger.warning("failed to call tracker server, status code=" + httpResponse.getStatus());
            return peerList;
        }

        BDecoderV2 bDecoderV2 = new BDecoderV2(httpResponse.getBytes());
        ValueWrapper trackerVW = bDecoderV2.decode();
        if (Objects.isNull(trackerVW)
                || !Objects.equals(trackerVW.getbEncodeType(), BEncodeTypeEnum.DICT)
                || !(trackerVW.getO() instanceof Map<?, ?> trackerVWMap)) {
            logger.warning("invalid tracker value wrapper, ignore parsing");
            return peerList;
        }

        ValueWrapperMap valueWrapperHelper = new ValueWrapperMap(trackerVWMap);
        if (!valueWrapperHelper.getFailureReason().isBlank()) {
            System.out.println(valueWrapperHelper.getFailureReason());
            return peerList;
        }

        valueWrapperHelper.getPeers().forEach(System.out::println);

        peerList.addAll(valueWrapperHelper.getPeers());
        return peerList;
    }
}
