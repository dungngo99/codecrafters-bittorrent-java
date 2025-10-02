package handler;

import domain.ValueWrapper;
import service.BDecoder;
import util.ValueWrapperUtil;

public class DecodeCmdHandler implements CmdHandler {

    @Override
    public ValueWrapper getValueWrapper(String[] args) {
        if (args == null || args.length == 0) {
            throw new RuntimeException("DecodeCmdHandler.getValueWrapper(): handle, invalid args");
        }

        // decode the b-encoded input string
        String encodedValue = args[0];
        BDecoder bEncoder = new BDecoder(encodedValue);
        return bEncoder.decode();
    }

    @Override
    public Object handleValueWrapper(ValueWrapper vw) {
        Object o = ValueWrapperUtil.convertToObject(vw, Boolean.FALSE);
        System.out.println(gson.toJson(o));
        return o;
    }
}
