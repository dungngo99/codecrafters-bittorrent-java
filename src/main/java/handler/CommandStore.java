package handler;

import enums.CommandTypeEnum;
import exception.InvalidCommandException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CommandStore {
    public static final Map<String, CommandHandler> store = new HashMap<>() {{
        put(CommandTypeEnum.DECODE.name().toLowerCase(), new DecodeHandler());
        put(CommandTypeEnum.INFO.name().toLowerCase(), new InfoHandler());
    }};

    public static CommandHandler getCommand(String command) {
        CommandHandler commandHandler = store.get(command);
        if (Objects.isNull(commandHandler)) {
            throw new InvalidCommandException(String.format("not found command=[%s]", command));
        }
        return commandHandler;
    }
}
