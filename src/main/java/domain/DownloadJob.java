package domain;

import java.util.Objects;

public class DownloadJob {
    private String pieceOutputFileOption;
    private String pieceOutputFilePath;
    private String torrentFilePath;
    private String pieceIndex;
    private String peerIndex;

    public String[] convertToArgs() {
        return Objects.nonNull(peerIndex)
                ? new String[]{pieceOutputFileOption, pieceOutputFilePath, torrentFilePath, pieceIndex, peerIndex}
                : new String[]{pieceOutputFileOption, pieceOutputFilePath, torrentFilePath, pieceIndex};
    }

    public String getPieceOutputFileOption() {
        return pieceOutputFileOption;
    }

    public void setPieceOutputFileOption(String pieceOutputFileOption) {
        this.pieceOutputFileOption = pieceOutputFileOption;
    }

    public String getPieceOutputFilePath() {
        return pieceOutputFilePath;
    }

    public void setPieceOutputFilePath(String pieceOutputFilePath) {
        this.pieceOutputFilePath = pieceOutputFilePath;
    }

    public String getTorrentFilePath() {
        return torrentFilePath;
    }

    public void setTorrentFilePath(String torrentFilePath) {
        this.torrentFilePath = torrentFilePath;
    }

    public String getPieceIndex() {
        return pieceIndex;
    }

    public void setPieceIndex(String pieceIndex) {
        this.pieceIndex = pieceIndex;
    }

    public String getPeerIndex() {
        return peerIndex;
    }

    public void setPeerIndex(String peerIndex) {
        this.peerIndex = peerIndex;
    }
}
