package domain;

import enums.BEncodeTypeEnum;

public class ValueWrapper {
    private BEncodeTypeEnum bEncodeType;
    private Object o;

    public ValueWrapper(BEncodeTypeEnum bEncodeType, Object o) {
        this.bEncodeType = bEncodeType;
        this.o = o;
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
