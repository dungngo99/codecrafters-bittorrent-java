package util;

import constants.Constant;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {

    public static String getRootPath() {
        return System.getProperty(Constant.USER_DIR_ROOT_PATH);
    }

    public static Path getPath(String path) {
        return Paths.get(getRootPath(), path);
    }

    public static File getFile(String path) {
        return new File(getPath(path).toString());
    }
}
