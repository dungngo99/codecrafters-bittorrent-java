import com.google.gson.Gson;
import domain.ValueWrapper;
import handler.CommandHandler;
import handler.CommandStore;

import java.util.Arrays;
//import com.dampcake.bencode.Bencode;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        if (args.length == 0) {
            return;
        }
        String command = args[0];
        try {
            CommandHandler commandHandler = CommandStore.getCommand(command);
            ValueWrapper vw = commandHandler.handle(Arrays.copyOfRange(args, 1, args.length));
            Object o = commandHandler.convert(vw);
            System.out.println(gson.toJson(o));
        } catch(RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }
}
