package util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static constants.Constant.*;

public class MagnetLinkUtil {

    public static Map<String, String> parseQueryParams(String queryParams) {
        Map<String, String> map = new HashMap<>();
        if (Objects.isNull(queryParams) || queryParams.isEmpty()) {
            return map;
        }

        String[] paramKVPairs = queryParams.split(AND_SIGN);
        for (String paramKVPair: paramKVPairs) {
            String[] paramKV = paramKVPair.split(EQUAL_SIGN);
            if (paramKV.length < PARAM_KV_PAIR_LENGTH) {
                continue;
            }
            String k = paramKV[0];
            String v = paramKV[1];
            map.put(k, v);
        }

        return map;
    }
}
