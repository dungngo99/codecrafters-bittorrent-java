package util;

import constants.Constant;
import exception.ArgumentException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DigestUtil {
    private static final Logger logger = Logger.getLogger(DigestUtil.class.getName());

    public static String calculateSHA1AsHex(byte[] bytes) {
        try {
            // obtain a MessageDigest instance for SHA-1
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");

            // Compute the hash
            byte[] hashBytes = sha1Digest.digest(bytes);

            // Convert byte array to a hexadecimal string
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "calculateSHA1AsHex: SHA-1 algorithm not found: " + e.getMessage());
            return null;
        }
    }

    public static byte[] calculateSHA1AsBytes(byte[] bytes) {
        try {
            // obtain a MessageDigest instance for SHA-1
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");

            // Compute then return the hash
            return sha1Digest.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "calculateSHA1AsBytes: SHA-1 algorithm not found: " + e.getMessage());
            return null;
        }
    }

    public static String formatHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    public static String[] formatPieceHashes(byte[] pieces) {
        int numPieces = pieces.length / Constant.PIECE_HASH_UNIT_LENGTH;
        String[] pieceHashes = new String[numPieces];

        for (int i = 0; i < numPieces; i++) {
            byte[] bytes = new byte[Constant.PIECE_HASH_UNIT_LENGTH];
            System.arraycopy(pieces, i * Constant.PIECE_HASH_UNIT_LENGTH, bytes, 0, Constant.PIECE_HASH_UNIT_LENGTH);
            pieceHashes[i] = formatHex(bytes);
        }

        return pieceHashes;
    }

    public static byte[] getBytesFromHex(String hex) {
        int hexLength = hex.length();
        if (hexLength % 2 != 0) {
            logger.severe("invalid hex string length=" + hexLength);
            throw new ArgumentException("invalid hex string length=" + hexLength + ", throw ex");
        }

        byte[] result = new byte[hexLength / 2];
        for (int i=0; i<hexLength; i+=2) {
            String hex_ = hex.substring(i, i+2);
            result[i/2] = (byte) Integer.parseInt(hex_, Constant.RADIX_HEX_TO_INT);
        }

        return result;
    }
}
