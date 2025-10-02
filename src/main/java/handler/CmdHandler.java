package handler;

import com.google.gson.Gson;
import domain.ValueWrapper;

public interface CmdHandler {
    Gson gson = new Gson();

    ValueWrapper getValueWrapper(String[] args);

    Object handleValueWrapper(ValueWrapper vw);
}
