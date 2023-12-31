package info.kgeorgiy.ja.belousov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Iterator;
import java.util.stream.Stream;

public class AbstractWalk {
    private static void writeResult(Writer output, String hash, String filePath) throws IOException {
        output.write(hash);
        output.write(' ');
        output.write(filePath);
        output.write(System.lineSeparator());
    }

    public static void solve(String in, String out, AbstractHasher hasher, IOFunction<Path, Stream<Path>> walker) {
        try {
            if (in == null) {
                throw new InvalidPathException("", "Input file is null");
            }
            final Path inputFile = Paths.get(in);
            try {
                if (out == null) {
                    throw new InvalidPathException("", "Output file is null");
                }
                final Path outputFile = Paths.get(out);

                Path parentDir = outputFile.getParent();
                if (parentDir != null && Files.notExists(parentDir)) {
                    try {
                        Files.createDirectories(outputFile.getParent());
                    } catch (IOException | UnsupportedOperationException e) {
                        System.err.print("Unable to create parent directory of an output file: ");
                        System.err.println(e.getMessage());
                    }
                }

                try (Stream<String> files = Files.lines(inputFile, StandardCharsets.UTF_8)) {
                    try (BufferedWriter output = new BufferedWriter(Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8))) {
                        Iterator<String> filesIter = files.iterator();

                        while (filesIter.hasNext()) {
                            String fileString = filesIter.next();
                            try {
                                Path rootFile = Paths.get(fileString);

                                try (Stream<Path> requestedFiles = walker.apply(rootFile)) {
                                    Iterator<Path> requestedFilesIterator = requestedFiles.iterator();
                                    if (!requestedFilesIterator.hasNext()) {
                                        throw new FileNotFoundException(fileString);
                                    }

                                    while (requestedFilesIterator.hasNext()) {
                                        Path file = requestedFilesIterator.next();
                                        if (!Files.isRegularFile(file)) continue;

                                        String result = hasher.getEmptyHash();
                                        try {
                                            hasher.setFile(file);
                                            result = hasher.compute();
                                        } catch (IOException e) {
                                            System.err.print("Error reading from file: ");
                                            System.err.println(e.getMessage());
                                        }
                                        writeResult(output, result, file.toString());
                                    }
                                }
                            } catch (InvalidPathException | IOException e) {
                                System.err.print("Could not access the requested file: ");
                                System.err.println(e.getMessage());
                                writeResult(output, hasher.getEmptyHash(), fileString);
                            }
                        }
                    } catch (IOException e) {
                        System.err.print("Unable to write to output file: ");
                        System.err.println(e.getMessage());
                    } catch (SecurityException e) {
                        System.err.print("Security violation writing to output file: ");
                        System.err.println(e.getMessage());
                    }
                } catch (NoSuchFileException e) {
                    System.err.print("Input file does not exist: ");
                    System.err.println(e.getMessage());
                } catch (IOException | UncheckedIOException e) {  // File Stream.HasNext wraps IOException into unchecked one (undocumented btw). Bad play, java!
                    System.err.print("Unable to read from input file: ");
                    System.err.println(e.getMessage());
                } catch (SecurityException e) {
                    System.err.print("Security violation reading from input file: ");
                    System.err.println(e.getMessage());
                }
            } catch (InvalidPathException e) {
                System.err.print("Input file path invalid: ");
                System.err.println(e.getMessage());
            }
        } catch (InvalidPathException e) {
            System.err.print("Output file path invalid: ");
            System.err.println(e.getMessage());
        }

    }
}
