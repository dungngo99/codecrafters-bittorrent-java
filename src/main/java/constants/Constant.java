package constants;

import enums.HttpMethod;

import java.util.Arrays;
import java.util.List;

public class Constant {
    public static final String USER_DIR_ROOT_PATH = "user.dir";
    public static final Integer DEFAULT_PARAMS_SIZE_INFO_CMD = 1;
    public static final Integer DEFAULT_PARAMS_SIZE_PEERS_CMD = 1;
    public static final Integer DEFAULT_PARAMS_SIZE_HANDSHAKE_CMD = 2;
    public static final Integer DEFAULT_PARAMS_SIZE_DOWNLOAD_PIECE_CMD = 4;
    public static final String ANNOUNCE_KEY_INFO_CMD = "announce";
    public static final String CREATED_BY_KEY_INFO_CMD = "created by";
    public static final String INFO_KEY_INFO_CMD = "info";
    public static final String INFO_LENGTH_KEY_INFO_CMD = "length";
    public static final String INFO_NAME_KEY_INFO_CMD = "name";
    public static final String INFO_PIECE_LENGTH_INFO_CMD = "piece length";
    public static final String INFO_PIECES_INFO_CMD = "pieces";
    public static final String PEERS_CMD = "peers";
    public static final String INTERVAL_PEERS_CMD = "interval";
    public static final String FAILURE_REASON_PEERS_CMD = "failure reason";
    public static final Integer PIECE_HASH_UNIT_LENGTH = 20;
    public static final String INFO_HASH_QUERY_PARAM_KEY = "info_hash";
    public static final String PEER_ID_QUERY_PARAM_KEY = "peer_id";
    public static final String PORT_QUERY_PARAM_KEY = "port";
    public static final String UPLOADED_QUERY_PARAM_KEY = "uploaded";
    public static final String DOWNLOADED_QUERY_PARAM_KEY = "downloaded";
    public static final String TORRENT_FILE_VALUE_WRAPPER_KEY = "torrent_file_value_wrapper";
    public static final String HANDSHAKE_IP_PORT_VALUE_WRAPPER_KEY = "handshake_ip_port_value_wrapper";
    public static final String DOWNLOAD_PIECE_VALUE_WRAPPER_KEY = "download_piece_value_wrapper";
    public static final String DOWNLOAD_PIECE_OUTPUT_FILE_PATH_VALUE_WRAPPER_KEY = "download_piece_output_file_path_value_wrapper";
    public static final String DOWNLOAD_PIECE_INDEX_VALUE_WRAPPER_KEY = "download_piece_index_value_wrapper";
    public static final String LEFT_QUERY_PARAM_KEY = "left";
    public static final String COMPACT_QUERY_PARAM_KEY = "compact";
    public static final String DEFAULT_PORT_QUERY_PARAM_VALUE = "6881";
    public static final Long DEFAULT_UPLOADED_QUERY_PARAM_VALUE = 0L;
    public static final Long DEFAULT_DOWNLOADED_QUERY_PARAM_VALUE = 0L;
    public static final Integer DEFAULT_COMPACT_QUERY_PARAM_VALUE = 1;
    public static final String HTTPS_PROTOCOL = "https://";
    public static final String HTTP_PROTOCOL = "http://";
    public static final String AND_SIGN = "&";
    public static final String EQUAL_SIGN = "=";
    public static final String QUESTION_MARK_SIGN = "?";
    public static final String PERCENT_SIGN = "%";
    public static final String COLON_SIGN = ":";
    public static final String DOT_SIGN = ".";
    public static final String EMPTY_STRING = "";
    public static final int HTTP_RESPONSE_STATUS_SUCCESS_START = 200;
    public static final int HTTP_RESPONSE_STATUS_SUCCESS_END = 299;
    public static final List<String> REQUEST_METHODS = Arrays.asList(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name());
    public static final List<String> REQUEST_METHODS_WITH_PAYLOAD = Arrays.asList(
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name());
    public static final String INVALID_REQUEST_METHOD = "Valid Request Method must be entered.";
    public static final String NOT_ALLOWED_REQUEST_METHOD_WITH_REQUEST_PAYLOAD = "Request Method %s does not allow request payload.";
    public static final String FAILED_TO_INIT_HTTP_CONNECTION_WITH_ERROR = "Failed to initiate http connection with error %s";
    public static final String FAILED_TO_CLOSE_INPUT_STREAM_WITH_ERROR = "Failed to close input stream with error %s";
    public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 10000;
    public static final int DEFAULT_READ_TIMEOUT_MS = 10000;
    public static final String HTTP_HEADER_CHARSET_KEY = "charset";
    public static final String HTTP_HEADER_CONNECTION_SCHEME = "Connection";
    public static final String HTTP_HEADER_CONNECTION_SCHEME_CLOSE = "Close";
    public static final String PEER_ID_KEY = "peer_id";
    public static final Integer PEER_ID_LENGTH = 20;
    public static final Integer PEER_BYTE_ARRAY_LENGTH = 6;
    public static final Integer PEER_IP_ADDRESS_BYTE_ARRAY_LENGTH = 4;
    public static final Integer PEER_PORT_NUMBER_BYTE_ARRAY_LENGTH = 2;
    public static final Integer RADIX_HEX_TO_INT = 16;
    public static final Integer HANDSHAKE_IP_ADDRESS_INDEX = 0;
    public static final Integer HANDSHAKE_PORT_NUMBER_INDEX = 1;
    public static final Integer HANDSHAKE_HEADER_BYTE_LENGTH = 1;
    public static final Integer HANDSHAKE_BITTORRENT_PROTOCOL_STR_LENGTH = 19;
    public static final Integer HANDSHAKE_RESERVED_BYTE_LENGTH = 8;
    public static final Integer HANDSHAKE_INFO_HASH_BYTE_LENGTH = 20;
    public static final Integer HANDSHAKE_PEER_ID_BYTE_LENGTH = 20;
    public static final String HANDSHAKE_BITTORRENT_PROTOCOL_STR = "BitTorrent protocol";
    public static final Integer HANDSHAKE_PEER_ID_INDEX_IN_VW_LIST = 4;
    public static final String IP_ADDRESS_PORT_NUMBER_FORMAT = "%s:%s";
    public static final Integer PEER_MESSAGE_PREFIXED_LENGTH = 4;
    public static final Integer PEER_MESSAGE_ID_LENGTH = 1;
    public static final Integer PEER_MESSAGE_BLOCK_SIZE = 16384;
    public static final Integer PEER_MESSAGE_INTERESTED_PREFIXED_LENGTH = 1;
    public static final Integer PEER_MESSAGE_REQUEST_PREFIXED_LENGTH = 13;
}
