package info.kgeorgiy.ja.belousov.walk;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha256Hasher extends AbstractHasher {
    public Sha256Hasher() throws NoSuchAlgorithmException {
        super(MessageDigest.getInstance("SHA-256"));
    }

    @Override
    protected int getBlockSize() {
        return 512;
    }
}
