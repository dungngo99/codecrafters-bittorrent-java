package service;

import constants.Constant;
import enums.HttpMethod;
import domain.HttpRequestOption;
import domain.HttpResponse;
import exception.HttpException;
import util.HttpUtil;
import util.SerialUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static constants.Constant.*;

public class HttpClient {
    public static final HttpClient DEFAULT_HTTP_CLIENT = new HttpClient(DEFAULT_CONNECTION_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
    private static final Logger logger = Logger.getLogger(HttpClient.class.getName());
    private final int connectTimeoutMS;
    private final int readTimeoutMS;

    public HttpClient(int connectTimeoutMS, int readTimeoutMS) {
        this.connectTimeoutMS = connectTimeoutMS;
        this.readTimeoutMS = readTimeoutMS;
    }

    public HttpResponse get(String url, Map<String, String> headers, Map<String, String> params, HttpRequestOption option) {
        return doGet(url, headers, params, option);
    }

    private HttpResponse doGet(String url, Map<String, String> headers, Map<String, String> params, HttpRequestOption option) {
        if (Objects.nonNull(params)) {
            String queryParam = HttpUtil.formatQueryParams(params, option.isNeedUrlEncodeQueryParam());
            url = url + Constant.QUESTION_MARK_SIGN + queryParam;
        }
        return doRequest(HttpMethod.GET.name(), url, headers, null);
    }

    private HttpResponse doRequest(String method, String url, Map<String, String> headers, Object payload) {
        if (!REQUEST_METHODS.contains(method)) {
            throw new HttpException(INVALID_REQUEST_METHOD);
        }
        if (Objects.nonNull(payload) && !REQUEST_METHODS_WITH_PAYLOAD.contains(method)) {
            throw new HttpException(String.format(NOT_ALLOWED_REQUEST_METHOD_WITH_REQUEST_PAYLOAD, method));
        }

        HttpURLConnection connection = null;
        InputStream inputStream = null;
        HttpResponse httpResponse;

        try {
            connection = getHttpURLConnection(method, url, headers, payload);
            int responseCode = connection.getResponseCode();
            if (HttpUtil.isSuccessHttpRequest(responseCode)) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }
            httpResponse = new HttpResponse(responseCode, inputStream.readAllBytes());
            httpResponse.getHeaders().putAll(HttpUtil.getResponseHeaders(connection));
        } catch (IOException e1) {
            throw new HttpException(String.format(FAILED_TO_INIT_HTTP_CONNECTION_WITH_ERROR, e1.getMessage()));
        } finally {
            if (Objects.nonNull(connection)) {
                connection.disconnect();
            }
            try {
                if (Objects.nonNull(inputStream)) {
                    inputStream.close();
                }
            } catch (IOException e2) {
                logger.log(Level.WARNING, String.format(FAILED_TO_CLOSE_INPUT_STREAM_WITH_ERROR, e2.getMessage()));
            }
        }

        return httpResponse;
    }

    private HttpURLConnection getHttpURLConnection(String method, String url, Map<String, String> headers, Object payload) throws IOException {
        URL urlObject = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObject.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(connectTimeoutMS);
        conn.setReadTimeout(readTimeoutMS);
        conn.setRequestProperty(HTTP_HEADER_CHARSET_KEY, StandardCharsets.UTF_8.name());
        conn.setRequestProperty(HTTP_HEADER_CONNECTION_SCHEME, HTTP_HEADER_CONNECTION_SCHEME_CLOSE);
        HttpUtil.fillRequestPropertiesWithHeaders(conn, headers);

        if (HttpMethod.PUT.name().equals(method) || HttpMethod.POST.name().equals(method) || HttpMethod.PATCH.name().equals(method)) {
            byte[] payloadBytes;
            if (payload instanceof byte[]) {
                payloadBytes = (byte[]) payload;
            } else {
                payloadBytes = SerialUtil.serialize(payload);
            }
            conn.getOutputStream().write(payloadBytes);
        }

        return conn;
    }
}
