package info.kgeorgiy.ja.belousov.walk;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public abstract class AbstractHasher {
    final protected MessageDigest hasher;
    private Path file;
    private final byte[] buffer;

    protected AbstractHasher(MessageDigest hasher) {
        this.hasher = hasher;
        this.buffer = new byte[getBlockSize()];
    }

    public String compute() throws IOException {
        try (BufferedInputStream file = new BufferedInputStream(Files.newInputStream(this.file))) {
            int bytesRead;
            while ((bytesRead = file.read(buffer)) > 0) {
                hasher.update(buffer, 0, bytesRead);
            }
        }

        byte[] digest = hasher.digest();
        StringBuilder result = new StringBuilder(hasher.getDigestLength() * 2);
        for (byte i : digest) {
            result.append(String.format("%02x", i));
        }
        return result.toString();
    }

    protected abstract int getBlockSize();

    protected int getDigestSize() {
        return hasher.getDigestLength();
    }

    public void setFile(Path file) {
        this.file = file;
        this.hasher.reset();
    }

    public String getEmptyHash() {
        return "0".repeat(getDigestSize() * 2);
    }
}
