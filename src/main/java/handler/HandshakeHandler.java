package handler;

import domain.ValueWrapper;
import enums.BEncodeTypeEnum;
import enums.CommandTypeEnum;
import exception.ArgumentException;
import service.BDecoderV2;
import util.ValueWrapperUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static constants.Constant.*;

public class HandshakeHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(HandshakeHandler.class.getName());

    @Override
    public ValueWrapper getValueWrapper(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_HANDSHAKE_CMD) {
            throw new ArgumentException("HandshakeHandler.getValueWrapper(): invalid params, ignore handling: args=" + Arrays.toString(args));
        }
        String torrentFilePath = args[0];
        String inputIpAddressPortNumber = args[1];

        CommandHandler commandHandler = CommandStore.getCommand(CommandTypeEnum.INFO.name().toLowerCase());
        ValueWrapper infoVW = commandHandler.getValueWrapper(new String[]{torrentFilePath});
        ValueWrapper inputIpAddressPortNumberVW = new ValueWrapper(BEncodeTypeEnum.STRING, inputIpAddressPortNumber);
        Map<String, ValueWrapper> handshakeVWMap = Map.of(
                TORRENT_FILE_VALUE_WRAPPER_KEY, infoVW,
                HANDSHAKE_IP_PORT_VALUE_WRAPPER_KEY, inputIpAddressPortNumberVW);
        return new ValueWrapper(BEncodeTypeEnum.DICT, handshakeVWMap);
    }

    @Override
    public void handleValueWrapper(ValueWrapper vw) {
        Object o1 = ValueWrapperUtil.convertToObject(vw);
        if (!(o1 instanceof Map<?, ?> handshakeMap)) {
            logger.warning("HandshakeHandler.handleValueWrapper(): invalid decoded value, ignore");
            return;
        }
        byte[] handshakeByteStreams = ValueWrapperUtil.getHandshakeByteStream(vw);

        String ipAddressPortNumber = (String) handshakeMap.get(HANDSHAKE_IP_PORT_VALUE_WRAPPER_KEY);
        String ipAddress = ipAddressPortNumber.split(COLON_SIGN)[HANDSHAKE_IP_ADDRESS_INDEX];
        String portNumber = ipAddressPortNumber.split(COLON_SIGN)[HANDSHAKE_PORT_NUMBER_INDEX];

        Socket socket = null;
        try {
            socket = new Socket(ipAddress, Integer.parseInt(portNumber));
            OutputStream os = socket.getOutputStream();
            os.write(handshakeByteStreams);
            os.flush();

            InputStream is = socket.getInputStream();
            BDecoderV2 bDecoderV2 = new BDecoderV2(is.readAllBytes());
            ValueWrapper handshakeVW = bDecoderV2.decodeHandshake();
            List<ValueWrapper> handshakeVWList = (List<ValueWrapper>) handshakeVW.getO();
            System.out.println(handshakeVWList.get(HANDSHAKE_PEER_ID_INDEX_IN_VW_LIST).getO());

        } catch (IOException e) {
            logger.warning(String.format("HandshakeHandler.handleValueWrapper(): failed to init TCP connection due to %s: host=%s; port=%s, ignored",
                    e.getMessage(), ipAddress, portNumber));
        } finally {
            if (Objects.nonNull(socket)) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.warning(String.format("HandshakeHandler.handleValueWrapper(): failed to close TCP connection due to %s: host=%s; port=%s, ignored",
                            e.getMessage(), ipAddress, portNumber));
                }
            }
        }
    }
}
