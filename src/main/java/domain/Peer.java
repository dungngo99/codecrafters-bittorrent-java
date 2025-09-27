package domain;

import constants.Constant;

public class Peer {
    private String ip;
    private Integer port;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return ip + Constant.COLON_SIGN + port;
    }
}
