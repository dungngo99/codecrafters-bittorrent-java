package handler;

import domain.ValueWrapper;

public interface CmdHandler {

    ValueWrapper getValueWrapper(String[] args);

    Object handleValueWrapper(ValueWrapper vw);
}
