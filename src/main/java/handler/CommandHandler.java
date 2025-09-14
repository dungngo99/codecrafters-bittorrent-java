package handler;

import domain.ValueWrapper;

public interface CommandHandler {
    ValueWrapper handle(String[] args);

    Object convert(ValueWrapper vw);
}
