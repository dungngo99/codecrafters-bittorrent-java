package handler;

import com.google.gson.Gson;
import domain.ValueWrapper;

public interface CommandHandler {
    Gson gson = new Gson();

    ValueWrapper getValueWrapper(String[] args);

    void handleValueWrapper(ValueWrapper vw);
}
