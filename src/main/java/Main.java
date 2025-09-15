import domain.ValueWrapper;
import handler.CommandHandler;
import handler.CommandStore;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            return;
        }
        String command = args[0];
        try {
            CommandHandler commandHandler = CommandStore.getCommand(command);
            ValueWrapper vw = commandHandler.getValueWrapper(Arrays.copyOfRange(args, 1, args.length));
            commandHandler.handleValueWrapper(vw);
        } catch(RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }
}
