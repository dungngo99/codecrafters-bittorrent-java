package service;

import domain.ValueWrapper;
import enums.BEncodeTypeEnum;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class BDecoder {
    private static final Logger logger = Logger.getLogger(BDecoder.class.getName());

    public static Object decode(ValueWrapper vw) {
        return decode(vw, Boolean.FALSE);
    }

    public static Object decode(ValueWrapper vw, boolean needConvertString) {
        if (Objects.isNull(vw)) {
            logger.warning("DecodeHandler: convert, null vw, ignore");
            return null;
        }
        BEncodeTypeEnum typeEnum = vw.getbEncodeType();
        if (Objects.equals(typeEnum, BEncodeTypeEnum.INTEGER)) {
            return vw.getO();
        }
        if (Objects.equals(typeEnum, BEncodeTypeEnum.STRING)) {
            // if use BEncoder, set needConvertString = false
            return needConvertString ? new String((byte[]) vw.getO(), StandardCharsets.UTF_8) : vw.getO();
        }
        if (Objects.equals(typeEnum, BEncodeTypeEnum.LIST)) {
            if (!(vw.getO() instanceof List<?>)) {
                logger.warning("DecodeHandler: convert, object not BEncodeTypeEnum.LIST, ignore");
                return null;
            }
            List<Object> list = new ArrayList<>();
            for (Object vw_ : (List<?>) vw.getO()) {
                list.add(decode((ValueWrapper) vw_, needConvertString));
            }
            return list;
        }
        if (Objects.equals(typeEnum, BEncodeTypeEnum.DICT)) {
            if (!(vw.getO() instanceof Map<?, ?>)) {
                logger.warning("DecodeHandler: convert, object not BEncodeTypeEnum.DICT, ignore");
                return null;
            }
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) vw.getO()).entrySet()) {
                map.put((String) entry.getKey(), decode((ValueWrapper) entry.getValue(), needConvertString));
            }
            return map;
        }
        return null;
    }
}