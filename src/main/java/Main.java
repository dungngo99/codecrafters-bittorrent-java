import handler.HybridCmdStore;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            return;
        }
        String cmd = args[0];
        String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);
        try {
            HybridCmdStore.handleCmd(cmd, cmdArgs);
        } catch(RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }
}
