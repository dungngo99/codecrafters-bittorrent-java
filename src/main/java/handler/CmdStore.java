package handler;

import enums.CmdTypeEnum;
import exception.InvalidCmdException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CmdStore {
    public static final Map<String, CmdHandler> store = new HashMap<>() {{
        put(CmdTypeEnum.DECODE.name().toLowerCase(), new DecodeCmdHandler());
        put(CmdTypeEnum.INFO.name().toLowerCase(), new InfoCmdHandler());
        put(CmdTypeEnum.PEERS.name().toLowerCase(), new PeersCmdHandler());
        put(CmdTypeEnum.HANDSHAKE.name().toLowerCase(), new HandshakeCmdHandler());
        put(CmdTypeEnum.DOWNLOAD_PIECE.name().toLowerCase(), new DownloadPieceCmdHandler());
        put(CmdTypeEnum.DOWNLOAD.name().toLowerCase(), new DownloadCmdHandler());
        put(CmdTypeEnum.MAGNET_PARSE.name().toLowerCase(), new MagnetParseCmdHandler());
    }};

    public static CmdHandler getCmd(String cmd) {
        CmdHandler cmdHandler = store.get(cmd);
        if (Objects.isNull(cmdHandler)) {
            throw new InvalidCmdException(String.format("CmdStore.getCmd(): not found cmd=[%s]", cmd));
        }
        return cmdHandler;
    }
}
