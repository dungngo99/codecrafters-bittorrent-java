package util;

import java.io.IOException;
import java.io.OutputStream;

public class SocketUtil {

    public static void writeThenFlush(OutputStream os, byte[] bytes) throws IOException {
        os.write(bytes);
        os.flush();
    }
}
