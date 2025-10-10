package domain;

import enums.TypeEnum;

public class ValueWrapper {
    private TypeEnum bEncodeType;
    private Object o;

    public ValueWrapper(TypeEnum bEncodeType, Object o) {
        this.bEncodeType = bEncodeType;
        this.o = o;
    }

    public TypeEnum getbEncodeType() {
        return bEncodeType;
    }

    public void setbEncodeType(TypeEnum bEncodeType) {
        this.bEncodeType = bEncodeType;
    }

    public Object getO() {
        return o;
    }

    public void setO(Object o) {
        this.o = o;
    }
}
