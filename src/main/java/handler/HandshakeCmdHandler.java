package handler;

import domain.ValueWrapper;
import enums.TypeEnum;
import enums.CmdType;
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
        String ipAddressPortNumber = args[1];

        // get .torrent file info from INFO cmd
        CmdHandler infoCmdHandler = HybridCmdStore.getCmdHandler(CmdType.INFO.name().toLowerCase());
        ValueWrapper torrentFileVW = infoCmdHandler.getValueWrapper(new String[]{torrentFilePath});

        // combine args and .torrent file info for next stage
        byte[] infoHashBytes = ValueWrapperUtil.getInfoHashAsBytes(torrentFileVW);
        String clientPeerId = PeerUtil.getSetPeerId();
        return ValueWrapperUtil.createHandshakeVW(ipAddressPortNumber, infoHashBytes, clientPeerId);
    }

    @Override
    public Object handleValueWrapper(ValueWrapper vw) {
        Object o1 = ValueWrapperUtil.convertToObject(vw);
        if (!(o1 instanceof Map<?, ?> handshakeMap)) {
            logger.warning("invalid decoded value, throw ex");
            throw new ValueWrapperException("HandshakeHandler.handleValueWrapper(): invalid decoded value");
        }

        String ipAddressPortNumber = (String) handshakeMap.get(HANDSHAKE_IP_PORT_VALUE_WRAPPER_KEY);
        String ipAddress = ipAddressPortNumber.split(COLON_SIGN)[HANDSHAKE_IP_ADDRESS_INDEX];
        String portNumber = ipAddressPortNumber.split(COLON_SIGN)[HANDSHAKE_PORT_NUMBER_INDEX];
        String clientPeerId = (String) handshakeMap.get(HANDSHAKE_CLIENT_PEER_ID_VALUE_WRAPPER_KEY);
        byte[] infoHashBytes = (byte[]) handshakeMap.get(HANDSHAKE_INFO_HASH_BYTES_VALUE_WRAPPER_KEY);
        Long reservedOption = (Long) handshakeMap.get(HANDSHAKE_RESERVED_OPTION_VALUE_WRAPPER_KEY);
        byte[] handshakeByteStream = PeerUtil.getHandshakeByteStream(clientPeerId, infoHashBytes, reservedOption);

        Map<String, ValueWrapper> connectionMap = new HashMap<>();
        try {
            Socket socket = new Socket(ipAddress, Integer.parseInt(portNumber));
            OutputStream os = socket.getOutputStream();
            os.write(handshakeByteStream);
            os.flush();

            InputStream is = socket.getInputStream();
            ValueWrapper handshakeVW = PeerUtil.decodeHandshake(is);
            List<ValueWrapper> handshakeVWList = (List<ValueWrapper>) handshakeVW.getO();
            ValueWrapper reservedOptionVW = handshakeVWList.get(HANDSHAKE_PEER_RESERVED_OPTION_INDEX_IN_VW_LIST);
            ValueWrapper peerIdVW = handshakeVWList.get(HANDSHAKE_PEER_ID_INDEX_IN_VW_LIST);
            ValueWrapper peerSocketVW = new ValueWrapper(TypeEnum.OBJECT, socket);

            String peerId = (String) peerIdVW.getO();
            System.out.println("Peer ID: " + peerId);

            connectionMap.put(HANDSHAKE_PEER_RESERVED_OPTION, reservedOptionVW);
            connectionMap.put(HANDSHAKE_PEER_ID, peerIdVW);
            connectionMap.put(HANDSHAKE_PEER_SOCKET_CONNECTION, peerSocketVW);
        } catch (IOException e) {
            logger.warning(String.format("failed to init TCP connection due to %s: host=%s; port=%s, throw ex",
                    e.getMessage(), ipAddress, portNumber));
            throw new PeerExchangeException(e);
        }

        return connectionMap;
    }
}
