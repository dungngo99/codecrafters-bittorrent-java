package util;

import constants.Constant;

import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class HttpUtil {

    public static String formatQueryParams(Map<String, String> params, boolean needEncode) {
        StringJoiner joiner = new StringJoiner(Constant.AND_SIGN);
        params.forEach((k, v) -> {
            v = needEncode ? URLEncoder.encode(v, StandardCharsets.UTF_8) : v;
            joiner.add(k + Constant.EQUAL_SIGN + v);
        });
        return joiner.toString();
    }

    public static boolean isSuccessHttpRequest(int statusCode) {
        return Constant.HTTP_RESPONSE_STATUS_SUCCESS_START <= statusCode && statusCode <= Constant.HTTP_RESPONSE_STATUS_SUCCESS_END;
    }

    public static Map<String, String> getResponseHeaders(HttpURLConnection conn) {
        Map<String, String> responseHeaderMap = new HashMap<>();
        if (Objects.isNull(conn)) {
            return responseHeaderMap;
        }
        int i = 0;
        while (conn.getHeaderFieldKey(i) != null) {
            responseHeaderMap.put(conn.getHeaderFieldKey(i), conn.getHeaderField(i));
            i++;
        }
        return responseHeaderMap;
    }

    public static void fillRequestPropertiesWithHeaders(HttpURLConnection conn, Map<String, String> headers) {
        if (Objects.isNull(conn) || MapUtil.isEmpty(headers)) {
            return;
        }
        headers.forEach(conn::setRequestProperty);
    }
}
