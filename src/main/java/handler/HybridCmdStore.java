package handler;

import domain.ValueWrapper;
import enums.CmdType;
import exception.InvalidCmdException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HybridCmdStore {
    public static final Map<String, CmdHandler> store = new HashMap<>() {{
        put(CmdType.DECODE.name().toLowerCase(), new DecodeCmdHandler());
        put(CmdType.INFO.name().toLowerCase(), new InfoCmdHandler());
        put(CmdType.PEERS.name().toLowerCase(), new PeersCmdHandler());
        put(CmdType.HANDSHAKE.name().toLowerCase(), new HandshakeCmdHandler());
        put(CmdType.DOWNLOAD_PIECE.name().toLowerCase(), new DownloadPieceCmdHandler());
        put(CmdType.DOWNLOAD.name().toLowerCase(), new DownloadCmdHandler());
    }};

    public static final Map<String, CmdHandlerV2> storeV2 = new HashMap<>() {{
        put(CmdType.MAGNET_PARSE.name().toLowerCase(), new MagnetParseCmdHandler());
        put(CmdType.MAGNET_HANDSHAKE.name().toLowerCase(), new MagnetHandshakeCmdHandler());
        put(CmdType.MAGNET_INFO.name().toLowerCase(), new MagnetInfoCmdHandler());
        put(CmdType.MAGNET_DOWNLOAD_PIECE.name().toLowerCase(), new MagnetDownloadPieceCmdHandler());
    }};

    public static void handleCmd(String cmd, String[] cmdArgs) {
        if (!store.containsKey(cmd) && !storeV2.containsKey(cmd)) {
            throw new InvalidCmdException(String.format("CmdStore.handleCmd(): not found cmd=[%s]", cmd));
        }
        if (store.containsKey(cmd)) {
            CmdHandler cmdHandler = getCmdHandler(cmd);
            ValueWrapper vw = cmdHandler.getValueWrapper(cmdArgs);
            cmdHandler.handleValueWrapper(vw);
        } else {
            CmdHandlerV2 cmdHandlerV2 = getCmdHandlerV2(cmd);
            cmdHandlerV2.handleCmdHandlerV2(cmdArgs);
        }
    }

    public static CmdHandler getCmdHandler(String cmd) {
        CmdHandler cmdHandler = store.get(cmd);
        if (Objects.isNull(cmdHandler)) {
            throw new InvalidCmdException(String.format("CmdStore.getCmdHandler(): not found cmd=[%s]", cmd));
        }
        return cmdHandler;
    }

    public static CmdHandlerV2 getCmdHandlerV2(String cmd) {
        CmdHandlerV2 cmdHandlerV2 = storeV2.get(cmd);
        if (Objects.isNull(cmdHandlerV2)) {
            throw new InvalidCmdException(String.format("CmdStore.getCmdHandlerV2(): not found cmd=[%s]", cmd));
        }
        return cmdHandlerV2;
    }
}
