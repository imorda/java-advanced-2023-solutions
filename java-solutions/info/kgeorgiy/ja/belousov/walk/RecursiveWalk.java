package info.kgeorgiy.ja.belousov.walk;

import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;

public class RecursiveWalk {
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Incorrect args!");
            System.err.println("Usage: java RecursiveWalk <input file path> <output file path>");
            return;
        }

        try {
            AbstractWalk.solve(args[0], args[1], new Sha256Hasher(), Files::walk);
        } catch (NoSuchAlgorithmException ignored) {
        }
    }
}
