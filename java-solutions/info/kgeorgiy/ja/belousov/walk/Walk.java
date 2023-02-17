package info.kgeorgiy.ja.belousov.walk;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

public class Walk {
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Incorrect args!");
            System.err.println("Usage: java Walk <input file path> <output file path>");
            return;
        }

        try {
            AbstractWalk.solve(args[0], args[1], new Sha256Hasher(), (Path root) -> {
                if (Files.exists(root) && Files.isRegularFile(root)) {
                    return Stream.of(root);
                }
                return Stream.empty();
            });
        } catch (NoSuchAlgorithmException ignored) {
        }
    }
}
