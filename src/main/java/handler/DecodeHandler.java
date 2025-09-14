package handler;

import domain.ValueWrapper;
import enums.BEncodeTypeEnum;
import service.BEncoder;

import java.util.*;
import java.util.logging.Logger;

public class DecodeHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(DecodeHandler.class.getName());

    @Override
    public ValueWrapper handle(String[] args) {
        if (args == null || args.length == 0) {
            throw new RuntimeException("DecodeHandler: handle, invalid args");
        }
        String encodedValue = args[1];
        BEncoder bEncoder = new BEncoder(encodedValue);
        return bEncoder.decode();
    }

    @Override
    public Object convert(ValueWrapper vw) {
        if (Objects.isNull(vw)) {
            logger.warning("DecodeHandler: convert, null vw, ignore");
            return null;
        }
        BEncodeTypeEnum typeEnum = vw.getbEncodeType();
        if (Objects.equals(typeEnum, BEncodeTypeEnum.INTEGER) || Objects.equals(typeEnum, BEncodeTypeEnum.STRING)) {
            return vw.getO();
        }
        if (Objects.equals(typeEnum, BEncodeTypeEnum.LIST)) {
            if (!(vw.getO() instanceof List<?>)) {
                logger.warning("DecodeHandler: convert, object not BEncodeTypeEnum.LIST, ignore");
                return null;
            }
            List<Object> list = new ArrayList<>();
            for (Object vw_: (List<?>) vw.getO()) {
                list.add(convert((ValueWrapper) vw_));
            }
            return list;
        }
        if (Objects.equals(typeEnum, BEncodeTypeEnum.DICT)) {
            if (!(vw.getO() instanceof Map<?,?>)) {
                logger.warning("DecodeHandler: convert, object not BEncodeTypeEnum.DICT, ignore");
                return null;
            }
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?,?> entry: ((Map<?,?>) vw.getO()).entrySet()) {
                map.put((String) entry.getKey(), convert((ValueWrapper) entry.getValue()));
            }
            return map;
        }
        return null;
    }
}
