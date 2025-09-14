package handler;

import constants.Constant;
import domain.ValueWrapper;
import exception.ArgumentException;
import util.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Objects;

public class InfoHandler implements CommandHandler {
    @Override
    public ValueWrapper handle(String[] args) {
        if (Objects.isNull(args) || args.length < Constant.DEFAULT_PARAMS_SIZE_INFO_CMD) {
            throw new ArgumentException("InfoHandler.handle(): invalid params, ignore handling: args=" + Arrays.toString(args));
        }
        String path = args[0];
        File file = FileUtil.getFile(path);
        return null;
    }

    @Override
    public Object convert(ValueWrapper vw) {
        return null;
    }
}
