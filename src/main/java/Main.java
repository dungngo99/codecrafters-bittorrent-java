import domain.ValueWrapper;
import handler.CmdHandler;
import handler.CmdStore;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            return;
        }
        String cmd = args[0];
        try {
            CmdHandler cmdHandler = CmdStore.getCmd(cmd);
            ValueWrapper vw = cmdHandler.getValueWrapper(Arrays.copyOfRange(args, 1, args.length));
            cmdHandler.handleValueWrapper(vw);
        } catch(RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }
}
