package handler;

import domain.MagnetLinkV1;
import domain.ValueWrapper;
import enums.TypeEnum;
import exception.ArgumentException;
import exception.MagnetLinkException;
import util.HttpUtil;
import util.MagnetLinkUtil;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static constants.Constant.*;

public class MagnetParseCmdHandler implements CmdHandler {
    private static final Logger logger = Logger.getLogger(MagnetParseCmdHandler.class.getName());

    @Override
    public ValueWrapper getValueWrapper(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_MAGNET_PARSE_CMD) {
            throw new ArgumentException("MagnetParseCmdHandler.getValueWrapper(): invalid params, args=" + Arrays.toString(args));
        }
        MagnetLinkV1 magnetLinkV1 = new MagnetLinkV1();
        String magnetLink = args[0];
        int index = 0;

        String scheme = magnetLink.substring(index, index + MAGNET_PROTOCOL_LENGTH);
        index += MAGNET_PROTOCOL_LENGTH;
        magnetLinkV1.setScheme(scheme);

        index++; // skip character `?`
        String queryParams = magnetLink.substring(index);
        Map<String, String> magnetLinkKVQueryParams = MagnetLinkUtil.parseQueryParams(queryParams);

        magnetLinkV1.setXt(magnetLinkKVQueryParams.get(MAGNET_PROTOCOL_XT_QUERY_KEY));
        magnetLinkV1.setDn(magnetLinkKVQueryParams.get(MAGNET_PROTOCOL_DN_QUERY_KEY));
        magnetLinkV1.setTr(magnetLinkKVQueryParams.get(MAGNET_PROTOCOL_TR_QUERY_KEY));

        return new ValueWrapper(TypeEnum.OBJECT, magnetLinkV1);
    }

    @Override
    public Object handleValueWrapper(ValueWrapper vw) {
        if (Objects.isNull(vw) || !Objects.equals(vw.getbEncodeType(), TypeEnum.OBJECT)) {
            logger.warning("invalid parsed value, throw ex");
            throw new MagnetLinkException("MagnetParseCmdHandler.handleValueWrapper(): invalid parsed value");
        }
        MagnetLinkV1 magnetLinkV1 = (MagnetLinkV1) vw.getO();

        String decodedTr = HttpUtil.decodeUrl(magnetLinkV1.getTr());
        magnetLinkV1.setDecodedTr(decodedTr);

        String infoHash = magnetLinkV1.getXt().substring(MAGNET_PROTOCOL_URN_BTIH_PREFIX);
        magnetLinkV1.setInfoHash(infoHash);

        System.out.println(magnetLinkV1);
        return magnetLinkV1;
    }
}
