package handler;

import domain.MagnetLinkV1;
import domain.PeerInfo;
import domain.PeerRequestQueryParam;
import domain.ValueWrapper;
import enums.CmdTypeEnum;
import enums.TypeEnum;
import exception.ArgumentException;
import exception.MagnetLinkException;
import util.BitUtil;
import util.DigestUtil;
import util.PeerUtil;
import util.ValueWrapperUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static constants.Constant.*;

public class MagnetHandshakeCmdHandler implements CmdHandler {

    @Override
    public ValueWrapper getValueWrapper(String[] args) {
        if (Objects.isNull(args) || args.length < DEFAULT_PARAMS_SIZE_MAGNET_HANDSHAKE_CMD) {
            throw new ArgumentException("MagnetHandshakeCmdHandler.getValueWrapper(): invalid params, args=" + Arrays.toString(args));
        }

        // parse magnet link
        CmdHandler magnetParseCmdHandler = CmdStore.getCmd(CmdTypeEnum.MAGNET_PARSE.name().toLowerCase());
        ValueWrapper vw = magnetParseCmdHandler.getValueWrapper(args);
        MagnetLinkV1 magnetLinkV1 = (MagnetLinkV1) magnetParseCmdHandler.handleValueWrapper(vw);

        // request track to get peer list
        PeerRequestQueryParam param = new PeerRequestQueryParam();
        param.setTrackerUrl(magnetLinkV1.getDecodedTr());
        param.setInfoHash(magnetLinkV1.getInfoHash());
        param.setInfoLength(String.valueOf(MAGNET_HANDSHAKE_DEFAULT_INFO_LENGTH));
        param.setPeerId(PeerUtil.getSetPeerId());
        List<PeerInfo> peerInfoList = PeerUtil.requestPeerInfoList(param);
        if (peerInfoList.isEmpty()) {
            throw new MagnetLinkException("MagnetHandshakeCmdHandler.getValueWrapper(): no peer found, args=" + Arrays.toString(args));
        }

        // establish connection and perform handshake
        int peerIndex = PeerUtil.randomizePeerBySize(peerInfoList.size());
        PeerInfo peerInfo = peerInfoList.get(peerIndex);
        String ipAddressPortNumber = String.format(IP_ADDRESS_PORT_NUMBER_FORMAT, peerInfo.getIp(), peerInfo.getPort());
        byte[] infoHashBytes = DigestUtil.getBytesFromHex(magnetLinkV1.getInfoHash());
        String clientPeerId = PeerUtil.getSetPeerId();
        Long reservedOption = BitUtil.set(INITIAL_OPTION, PEER_EXCHANGE_TORRENT_METADATA_EXTENSION_OPTION);
        ValueWrapper handshakeVW = ValueWrapperUtil.createHandshakeVW(ipAddressPortNumber, infoHashBytes, clientPeerId, reservedOption);

        CmdHandler handshakeCmdHandler = CmdStore.getCmd(CmdTypeEnum.HANDSHAKE.name().toLowerCase());
        Map<String, ValueWrapper> socketMap = (Map<String, ValueWrapper>) handshakeCmdHandler.handleValueWrapper(handshakeVW);

        return new ValueWrapper(TypeEnum.OBJECT, socketMap);
    }

    @Override
    public Object handleValueWrapper(ValueWrapper vw) {
        return null;
    }
}
