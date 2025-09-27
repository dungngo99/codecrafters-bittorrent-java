package domain;

public class TorrentFile {
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
        // not part of parsed info
        private String hash;
        // not part of parsed info
        private String[] pieceHashes;

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

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public String[] getPieceHashes() {
            return pieceHashes;
        }

        public void setPieceHashes(String[] pieceHashes) {
            this.pieceHashes = pieceHashes;
        }
    }

    @Override
    public String toString() {
        return "Tracker URL: " + announce +
                "\n" + "Length: " + info.length +
                "\n" + "Info Hash: " + info.hash +
                "\n" + "Piece Length: " + info.pieceLength +
                "\n" + "Piece Hashes:" + "\n" + String.join("\n", info.pieceHashes);
    }
}
