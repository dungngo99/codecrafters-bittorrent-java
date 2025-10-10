package util;

import java.nio.ByteBuffer;

public class ByteUtil {

    public static int getAsInt(byte[] bytes) {
        assert bytes.length == Integer.BYTES;
        return wrap(bytes).getInt();
    }

    public static ByteBuffer wrap(byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }

    public static byte[] getFromInt(int i) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(i).array();
    }

    public static byte[] getFromByte(byte b) {
        return ByteBuffer.allocate(Byte.BYTES).put(b).array();
    }

    public static byte[] getFromLong(long l) {
        return ByteBuffer.allocate(Long.BYTES).putLong(l).array();
    }

    public static void fill(byte[] out, byte[] in, int offset) {
        ByteBuffer byteBuffer = wrap(out);
        byteBuffer.position(offset);
        byteBuffer.put(in);
    }
}
