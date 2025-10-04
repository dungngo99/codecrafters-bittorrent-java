package util;

import constants.Constant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {

    public static String getRootPath() {
        return System.getProperty(Constant.USER_DIR_ROOT_PATH);
    }

    public static Path getPath(String path) {
        return Paths.get(path);
    }

    public static File getFile(String path) {
        return new File(getPath(path).toString());
    }

    public static void writeBytesToFile(String path, byte[] bytes, boolean append) throws IOException {
        File file = getFile(path);
        FileOutputStream fileOutputStream = new FileOutputStream(file, append);
        fileOutputStream.write(bytes);
    }

    public static byte[] readAllBytesFromFile(String path) throws IOException {
        File file = getFile(path);
        FileInputStream fileInputStream = new FileInputStream(file);
        return fileInputStream.readAllBytes();
    }

    public static boolean deleteFile(String path) {
        File file = getFile(path);
        return file.delete();
    }
}
