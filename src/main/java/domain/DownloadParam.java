package domain;

import java.net.Socket;
import java.util.List;

public class DownloadParam {
    private String peerId;
    private Socket socket;
    private int pieceIndex;
    private int infoPieceLength;
    private int infoLength;
    private String pieceOutputFilePath;
    private List<String> pieceHashList;

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public void setPieceIndex(int pieceIndex) {
        this.pieceIndex = pieceIndex;
    }

    public int getInfoPieceLength() {
        return infoPieceLength;
    }

    public void setInfoPieceLength(int infoPieceLength) {
        this.infoPieceLength = infoPieceLength;
    }

    public int getInfoLength() {
        return infoLength;
    }

    public void setInfoLength(int infoLength) {
        this.infoLength = infoLength;
    }

    public String getPieceOutputFilePath() {
        return pieceOutputFilePath;
    }

    public void setPieceOutputFilePath(String pieceOutputFilePath) {
        this.pieceOutputFilePath = pieceOutputFilePath;
    }

    public List<String> getPieceHashList() {
        return pieceHashList;
    }

    public void setPieceHashList(List<String> pieceHashList) {
        this.pieceHashList = pieceHashList;
    }

    @Override
    public String toString() {
        return "DownloadParam{" +
                "peerId='" + peerId + '\'' +
                ", pieceIndex=" + pieceIndex +
                ", infoPieceLength=" + infoPieceLength +
                ", infoLength=" + infoLength +
                ", pieceOutputFilePath='" + pieceOutputFilePath + '\'' +
                '}';
    }
}
