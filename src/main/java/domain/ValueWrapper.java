package domain;

import enums.Type;

public class ValueWrapper {
    private Type bEncodeType;
    private Object o;

    public ValueWrapper(Type bEncodeType, Object o) {
        this.bEncodeType = bEncodeType;
        this.o = o;
    }

    public Type getbEncodeType() {
        return bEncodeType;
    }

    public void setbEncodeType(Type bEncodeType) {
        this.bEncodeType = bEncodeType;
    }

    public Object getO() {
        return o;
    }

    public void setO(Object o) {
        this.o = o;
    }
}
