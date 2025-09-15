package handler;

import domain.ValueWrapper;
import service.BDecoder;
import service.BEncoder;

public class DecodeHandler implements CommandHandler {

    @Override
    public ValueWrapper getValueWrapper(String[] args) {
        if (args == null || args.length == 0) {
            throw new RuntimeException("DecodeHandler: handle, invalid args");
        }
        String encodedValue = args[0];
        BEncoder bEncoder = new BEncoder(encodedValue);
        return bEncoder.decode();
    }

    @Override
    public void handleValueWrapper(ValueWrapper vw) {
        Object o = BDecoder.decode(vw, Boolean.FALSE);
        System.out.println(gson.toJson(o));
    }
}
