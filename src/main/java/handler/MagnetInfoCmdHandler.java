package handler;

import domain.PeerMessage;
import domain.ValueWrapper;
import enums.CmdTypeEnum;
import exception.ArgumentException;
import exception.MagnetLinkException;
import util.PeerUtil;

import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static constants.Constant.*;

public class MagnetInfoCmdHandler implements CmdHandlerV2 {
    private static final Logger logger = Logger.getLogger(MagnetInfoCmdHandler.class.getName());

    @Override
    public Object handleCmdHandlerV2(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_MAGNET_PARSE_CMD) {
            throw new ArgumentException("MagnetInfoCmdHandler.handleCmdHandlerV2(): invalid params, args=" + Arrays.toString(args));
        }

        CmdHandlerV2 cmdHandlerV2 = HybridCmdStore.getCmdHandlerV2(CmdTypeEnum.MAGNET_HANDSHAKE.name().toLowerCase());
        ValueWrapper vw = (ValueWrapper) cmdHandlerV2.handleCmdHandlerV2(args);
        Map<String, ValueWrapper> handshakeMap = (Map<String, ValueWrapper>) vw.getO();
        Socket socket = (Socket) handshakeMap.get(HANDSHAKE_PEER_SOCKET_CONNECTION).getO();
        Integer peerMetadataExtensionID = (Integer) handshakeMap.get(EXTENSION_HANDSHAKE_UT_METADATA_KEY_NAME).getO();

        try {
            OutputStream os = socket.getOutputStream();
            PeerMessage peerMessage = PeerUtil.sendExtensionMetadataMessage(os, peerMetadataExtensionID);
            logger.info("send extension metadata message " + peerMessage);
        } catch (Exception e) {
            throw new MagnetLinkException("failed to perform extension metadata message due to error=" + e.getMessage());
        }
        return null;
    }
}
