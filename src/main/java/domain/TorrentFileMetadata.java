package domain;

public class TorrentFileMetadata {
    private String announce;
    private String createdBy;
    private Info info;

    public String getAnnounce() {
        return announce;
    }

    public void setAnnounce(String announce) {
        this.announce = announce;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public static class Info {
        private Integer length;
        private String name;
        private Integer pieceLength;
        private byte[] pieces;

        public Integer getLength() {
            return length;
        }

        public void setLength(Integer length) {
            this.length = length;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getPieceLength() {
            return pieceLength;
        }

        public void setPieceLength(Integer pieceLength) {
            this.pieceLength = pieceLength;
        }

        public byte[] getPieces() {
            return pieces;
        }

        public void setPieces(byte[] pieces) {
            this.pieces = pieces;
        }
    }

    @Override
    public String toString() {
        return "Tracker URL: " + announce + "\n" + "Length: " + info.length;
    }
}
