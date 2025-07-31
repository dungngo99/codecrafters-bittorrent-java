package domain;

import enums.BEncodeTypeEnum;

import java.util.*;

public class ValueWrapper {
    private BEncodeTypeEnum bEncodeType;
    private Object o;

    public ValueWrapper(BEncodeTypeEnum bEncodeType, Object o) {
        this.bEncodeType = bEncodeType;
        this.o = o;
    }

    public static Object convert(ValueWrapper vw) {
        if (Objects.isNull(vw)) {
            return null;
        }
        BEncodeTypeEnum typeEnum = vw.getbEncodeType();
        if (Objects.equals(typeEnum, BEncodeTypeEnum.INTEGER) || Objects.equals(typeEnum, BEncodeTypeEnum.STRING)) {
            return vw.getO();
        }
        if (Objects.equals(typeEnum, BEncodeTypeEnum.LIST)) {
            if (!(vw.getO() instanceof List<?>)) {
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

    public BEncodeTypeEnum getbEncodeType() {
        return bEncodeType;
    }

    public void setbEncodeType(BEncodeTypeEnum bEncodeType) {
        this.bEncodeType = bEncodeType;
    }

    public Object getO() {
        return o;
    }

    public void setO(Object o) {
        this.o = o;
    }
}
