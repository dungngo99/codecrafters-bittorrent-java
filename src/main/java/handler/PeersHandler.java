package handler;

import domain.HttpRequestOption;
import domain.HttpResponse;
import domain.ValueWrapper;
import enums.BEncodeTypeEnum;
import enums.CommandTypeEnum;
import exception.ArgumentException;
import service.BDecoderV2;
import service.HttpClient;
import service.ValueWrapperHelper;
import util.HttpUtil;
import util.ValueWrapperUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static constants.Constant.*;

public class PeersHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(PeersHandler.class.getName());

    @Override
    public ValueWrapper getValueWrapper(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_PEERS_CMD) {
            throw new ArgumentException("PeersHandler.getValueWrapper(): invalid params, ignore handling: args=" + Arrays.toString(args));
        }
        CommandHandler commandHandler = CommandStore.getCommand(CommandTypeEnum.INFO.name().toLowerCase());
        return commandHandler.getValueWrapper(args);
    }

    @Override
    public void handleValueWrapper(ValueWrapper vw) {
        Object o = ValueWrapperUtil.convertToObject(vw);
        if (!(o instanceof Map<?, ?> map)) {
            logger.warning("PeersHandler.handleValueWrapper(): invalid decoded value, ignore");
            return;
        }
        ValueWrapperHelper torrentFileHelper = new ValueWrapperHelper(map);
        Map<String, String> queryParams = new HashMap<>();
        String infoHash = torrentFileHelper.getInfoHash(vw);
        queryParams.put(INFO_HASH_QUERY_PARAM_KEY, torrentFileHelper.urlEncodeInfoHash(infoHash));
        queryParams.put(PEER_ID_QUERY_PARAM_KEY, torrentFileHelper.getSetPeerId());
        queryParams.put(PORT_QUERY_PARAM_KEY, DEFAULT_PORT_QUERY_PARAM_VALUE);
        queryParams.put(UPLOADED_QUERY_PARAM_KEY, String.valueOf(DEFAULT_UPLOADED_QUERY_PARAM_VALUE));
        queryParams.put(DOWNLOADED_QUERY_PARAM_KEY, String.valueOf(DEFAULT_DOWNLOADED_QUERY_PARAM_VALUE));
        queryParams.put(LEFT_QUERY_PARAM_KEY, String.valueOf(torrentFileHelper.getInfoLength()));
        queryParams.put(COMPACT_QUERY_PARAM_KEY, String.valueOf(DEFAULT_COMPACT_QUERY_PARAM_VALUE));

        String trackerUrl = torrentFileHelper.getAnnounce();
        HttpRequestOption option = new HttpRequestOption.Builder().ofNeedUrlEncodeQueryParam(Boolean.FALSE).build();
        HttpResponse response = HttpClient.DEFAULT_HTTP_CLIENT.get(trackerUrl, Map.of(), queryParams, option);
        if (Objects.isNull(response) || !HttpUtil.isSuccessHttpRequest(response.getStatus())) {
            logger.warning("PeersHandler.handleValueWrapper(): failed to call tracker server, status code=" + response.getStatus());
            return;
        }

        BDecoderV2 bDecoderV2 = new BDecoderV2(response.getBytes());
        ValueWrapper trackerVW = bDecoderV2.decode();
        if (Objects.isNull(trackerVW) || !Objects.equals(trackerVW.getbEncodeType(), BEncodeTypeEnum.DICT) || !(trackerVW.getO() instanceof Map<?, ?> trackerVWMap)) {
            logger.warning("PeersHandler.handleValueWrapper(): invalid tracker value wrapper, ignore parsing");
            return;
        }

        ValueWrapperHelper valueWrapperHelper = new ValueWrapperHelper(trackerVWMap);
        if (!valueWrapperHelper.getFailureReason().isBlank()) {
            System.out.println(valueWrapperHelper.getFailureReason());
            return;
        }

        valueWrapperHelper.getPeers().forEach(System.out::println);
    }
}
