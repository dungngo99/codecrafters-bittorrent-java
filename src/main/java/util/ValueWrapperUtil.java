package util;

import domain.ValueWrapper;
import enums.BEncodeTypeEnum;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class ValueWrapperUtil {
    private static final Logger logger = Logger.getLogger(ValueWrapperUtil.class.getName());

    public static Object convertToObject(ValueWrapper vw) {
        return convertToObject(vw, Boolean.FALSE);
    }

    public static Object convertToObject(ValueWrapper vw, boolean needConvertString) {
        if (Objects.isNull(vw)) {
            logger.warning("DecodeHandler: convert, null vw, ignore");
            return null;
        }
        BEncodeTypeEnum typeEnum = vw.getbEncodeType();
        if (Objects.equals(typeEnum, BEncodeTypeEnum.INTEGER)) {
            return vw.getO();
        }
        if (Objects.equals(typeEnum, BEncodeTypeEnum.STRING)) {
            // if use BDecoder, set needConvertString = false
            return needConvertString ? new String((byte[]) vw.getO(), StandardCharsets.UTF_8) : vw.getO();
        }
        if (Objects.equals(typeEnum, BEncodeTypeEnum.LIST)) {
            if (!(vw.getO() instanceof List<?>)) {
                logger.warning("DecodeHandler: convert, object not BEncodeTypeEnum.LIST, ignore");
                return null;
            }
            List<Object> list = new ArrayList<>();
            for (Object vw_ : (List<?>) vw.getO()) {
                list.add(convertToObject((ValueWrapper) vw_, needConvertString));
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
                map.put((String) entry.getKey(), convertToObject((ValueWrapper) entry.getValue(), needConvertString));
            }
            return map;
        }
        return null;
    }

    public static ValueWrapper getObjectFromMap(ValueWrapper vw, String key) {
        if (Objects.isNull(vw) || !(vw.getO() instanceof Map<?, ?> map)) {
            return null;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String entryKey = (String) entry.getKey();
            if (Objects.equals(entryKey, key)) {
                return (ValueWrapper) entry.getValue();
            }

            ValueWrapper entryValue = (ValueWrapper) entry.getValue();
            ValueWrapper vw_ = getObjectFromMap(entryValue, key);
            if (Objects.nonNull(vw_)) {
                return vw_;
            }
        }
        return null;
    }
}