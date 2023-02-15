package info.kgeorgiy.ja.belousov.walk;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha256Hasher extends AbstractHasher {
    private static final int BLOCK_SIZE = 512;

    public Sha256Hasher() throws NoSuchAlgorithmException {
        super(MessageDigest.getInstance("SHA-256"));
    }

    @Override
    protected byte[] getBuffer() {
        return new byte[BLOCK_SIZE];
    }
}
