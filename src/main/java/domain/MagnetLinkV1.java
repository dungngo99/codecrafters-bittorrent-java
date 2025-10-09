package domain;

public class MagnetLinkV1 {
    private String scheme;
    private String xt;
    private String dn;
    private String tr;
    private String infoHash;
    private String decodedTr;

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getXt() {
        return xt;
    }

    public void setXt(String xt) {
        this.xt = xt;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getTr() {
        return tr;
    }

    public void setTr(String tr) {
        this.tr = tr;
    }

    public String getInfoHash() {
        return infoHash;
    }

    public void setInfoHash(String infoHash) {
        this.infoHash = infoHash;
    }

    public String getDecodedTr() {
        return decodedTr;
    }

    public void setDecodedTr(String decodedTr) {
        this.decodedTr = decodedTr;
    }

    @Override
    public String toString() {
        return "Tracker URL: " + decodedTr + "\n"
                + "Info Hash: " + infoHash;
    }
}
