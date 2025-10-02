package handler;

import domain.ValueWrapper;
import enums.BEncodeTypeEnum;
import enums.CmdTypeEnum;
import exception.ArgumentException;
import exception.PeerExchangeException;
import exception.ValueWrapperException;
import util.PeerUtil;
import util.ValueWrapperUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

import static constants.Constant.*;

public class HandshakeCmdHandler implements CmdHandler {
    private static final Logger logger = Logger.getLogger(HandshakeCmdHandler.class.getName());

    @Override
    public ValueWrapper getValueWrapper(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_HANDSHAKE_CMD) {
            throw new ArgumentException("HandshakeHandler.getValueWrapper(): invalid params: args=" + Arrays.toString(args));
        }
        String torrentFilePath = args[0];
        String inputIpAddressPortNumber = args[1];

        // get .torrent file info from INFO cmd
        CmdHandler infoCmdHandler = CmdStore.getCmd(CmdTypeEnum.INFO.name().toLowerCase());
        ValueWrapper torrentFileVW = infoCmdHandler.getValueWrapper(new String[]{torrentFilePath});

        // combine args and .torrent file info for next stage
        ValueWrapper inputIpAddressPortNumberVW = new ValueWrapper(BEncodeTypeEnum.STRING, inputIpAddressPortNumber);
        Map<String, ValueWrapper> handshakeVWMap = Map.of(
                TORRENT_FILE_VALUE_WRAPPER_KEY, torrentFileVW,
                HANDSHAKE_IP_PORT_VALUE_WRAPPER_KEY, inputIpAddressPortNumberVW);
        return new ValueWrapper(BEncodeTypeEnum.DICT, handshakeVWMap);
    }

    @Override
    public Object handleValueWrapper(ValueWrapper vw) {
        Object o1 = ValueWrapperUtil.convertToObject(vw);
        if (!(o1 instanceof Map<?, ?> handshakeMap)) {
            logger.warning("HandshakeHandler.handleValueWrapper(): invalid decoded value, throw ex");
            throw new ValueWrapperException("HandshakeHandler.handleValueWrapper(): invalid decoded value");
        }
        byte[] handshakeByteStream = PeerUtil.getHandshakeByteStream(vw);

        String ipAddressPortNumber = (String) handshakeMap.get(HANDSHAKE_IP_PORT_VALUE_WRAPPER_KEY);
        String ipAddress = ipAddressPortNumber.split(COLON_SIGN)[HANDSHAKE_IP_ADDRESS_INDEX];
        String portNumber = ipAddressPortNumber.split(COLON_SIGN)[HANDSHAKE_PORT_NUMBER_INDEX];

        Socket socket = null;
        Map<String, Socket> connectionMap = new HashMap<>();
        try {
            socket = new Socket(ipAddress, Integer.parseInt(portNumber));
            OutputStream os = socket.getOutputStream();
            os.write(handshakeByteStream);
            os.flush();

            InputStream is = socket.getInputStream();
            ValueWrapper handshakeVW = PeerUtil.decodeHandshake(is);
            List<ValueWrapper> handshakeVWList = (List<ValueWrapper>) handshakeVW.getO();
            String peerId = (String) handshakeVWList.get(HANDSHAKE_PEER_ID_INDEX_IN_VW_LIST).getO();
            System.out.println("Peer ID: " + peerId);

            connectionMap.put(peerId, socket);
        } catch (IOException e) {
            logger.warning(String.format("HandshakeHandler.handleValueWrapper(): failed to init TCP connection due to %s: host=%s; port=%s, throw ex",
                    e.getMessage(), ipAddress, portNumber));
            throw new PeerExchangeException(e);
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

        return connectionMap;
    }
}
