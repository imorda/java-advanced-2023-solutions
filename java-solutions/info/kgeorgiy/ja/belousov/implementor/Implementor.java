package info.kgeorgiy.ja.belousov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * The Implementor class implements the {@link JarImpler} interface and provides methods for generating default
 * implementations of given interfaces and packaging them into a JAR file.
 * Can also be launched as a standalone program.
 *
 * @author Belousov Timofey
 */
public class Implementor implements JarImpler {
    /**
     * Main method of the Implementor class which provides the command-line interface for using the class.
     * Takes two arguments: the fully qualified name of the class to be implemented and the path to the output JAR file.
     * If the arguments are not provided, prints the usage instruction and returns.
     *
     * @param args command-line arguments split by a whitespace
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java Implementor <className> <outputJar>");
            return;
        }

        Implementor instance = new Implementor();

        String className = args[0];
        String outputJar = args[1];
        try {
            instance.implementJar(Class.forName(className), Paths.get(outputJar));
        } catch (InvalidPathException e) {
            System.err.println("Cannot resolve the requested output directory: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("Unable to implement the required class: " + e.getMessage());
        } catch (ExceptionInInitializerError e) {
            System.err.println("Unable to initialize the requested class: " + e.getMessage());
        } catch (LinkageError e) {
            System.err.println("Unable to link the requested class: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Cannot resolve the requested class name: " + e.getMessage());
        }
    }

    /**
     * Returns the fully qualified name of a given {@link Type}, replacing inner class separator characters with
     * package separator characters.
     *
     * @param type the {@link Type} whose fully qualified name to retrieve.
     * @return the fully qualified name of the given {@link Type}.
     */
    private static String getFullyQualifiedTypeName(Type type) {
        return type.getTypeName().replace('$', '.');
    }

    /**
     * Returns the name of the implementation class for a given {@link Class} token, appending the "Impl" suffix.
     *
     * @param token the {@link Class} token for which to retrieve the implementation name.
     * @return the name of the implementation class for the given {@link Class} token.
     */
    private static String getImplName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Returns the {@link Path} of the implementation file for a given {@link Class} token, relative to the given root
     * directory and with the given file extension.
     *
     * @param token     the {@link Class} token for which to retrieve the implementation file {@link Path}.
     * @param root      the classpath of the generated files.
     * @param extension the file extension of the implementation file (can be {@code ".java"} or {@code ".class"})
     * @return the {@link Path} of the implementation file for the given {@link Class} token.
     */
    private static Path getImplFile(Class<?> token, Path root, String extension) {
        return root.resolve(token.getPackageName().replace(".", File.separator))
                .resolve(getImplName(token) + extension);
    }

    /**
     * Returns the path of a generated implementation java source file, relative to the given root directory.
     *
     * @param token the class to be implemented.
     * @param root  the classpath of the generated files.
     * @return the path of a java source generated implementation file.
     */
    private static Path getImplPath(Class<?> token, Path root) {
        return getImplFile(token, root, ".java");
    }

    /**
     * Returns the path of a generated implementation java classfile, relative to the given root directory.
     *
     * @param token the class to be implemented.
     * @param root  the classpath of the generated files.
     * @return the path of a java generated implementation classfile.
     */
    private static Path getCompiledPath(Class<?> token, Path root) {
        return getImplFile(token, root, ".class");
    }

    /**
     * Returns the classpath of a given token.
     * If the token doesn't have a classpath (like in case it is a built-in java token), {@code null} is returned
     *
     * @param token the class to return the classpath of.
     * @return the classpath of a given token. Can be {@code null}.
     * @throws ImplerException if unable to obtain the classpath.
     */
    private static Path getContextClasspath(Class<?> token) throws ImplerException {
        try {
            CodeSource codeSource = token.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }
            return Path.of(codeSource.getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new ImplerException("Cannot obtain token classpath", e);
        }
    }

    /**
     * Compiles the generated Java source file.
     * Source files must be located at {@code srcFile}. The resulting class files are written to the
     * directory located at {@code out}. All dependant classes have to be located inside the {@code cp}.
     *
     * @param cp               the classpath directory of the generated source files
     * @param out              the directory where the compiled class files should be written to
     * @param srcFile          the path to the source file to compile
     * @param contextClasspath the classpath that includes all the {@code srcFile} dependant resources,
     *                         or {@code null} if none
     * @throws ImplerException if an error occurs during the compilation process or the compiler cannot be accessed.
     */
    private static void compile(Path cp, Path out, Path srcFile, Path contextClasspath) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Compiler not found");
        }

        String compilerClasspath = cp.toString();
        if (contextClasspath != null) {
            compilerClasspath += File.pathSeparator + contextClasspath;
        }

        final String[] args = {"-cp", compilerClasspath, "-d", out.toString(), srcFile.toString()};
        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException(String.format("Compiler returned %d code", exitCode));
        }
    }

    /**
     * Recursively deletes the specified file or directory.
     *
     * @param path the path to the file or directory to delete. {@code null} values are ignored.
     * @throws IOException if any I/O error occurs during the deletion process
     */
    private static void cleanup(Path path) throws IOException {
        if (path == null) {
            return;
        }
        if (Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Packs a single compiled implementation class file into a JAR archive.
     * Takes the implemented class token to extract a proper classpath to its dependencies.
     *
     * @param compileRoot the root classpath directory of the compiled class files
     * @param jarPath     the path to the JAR archive to create
     * @param implToken   the class or interface being implemented
     * @throws ImplerException          if any error occurs during the JAR packaging process
     * @throws IllegalArgumentException if the resulting jar file appears to have any entries that are too long
     */
    private void packSingleJar(Path compileRoot, Path jarPath, Class<?> implToken) throws ImplerException {
        try (JarOutputStream outputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
            Path compiledPath = getCompiledPath(implToken, Path.of(""));
            outputStream.putNextEntry(new JarEntry(compiledPath
                    .toString().replace(File.separator, "/")));
            Files.copy(compileRoot.resolve(compiledPath), outputStream);
        } catch (IOException e) {
            throw new ImplerException("Error writing to a jar file", e);
        }
    }

    /**
     * Generates a class implementing the provided interface and saves it to the specified classpath directory.
     * Generated class is provided as source java code, all implemented methods have the default implementation:
     * they all ignore any provided parameters and return:
     * <ul>
     *   <li>{@code null} in case of any non-primitive type</li>
     *   <li>{@code 0} in case of a primitive numeric type</li>
     *   <li>{@code false} in case of boolean</li>
     * </ul>
     * Only methods with no default implementation are generated.
     *
     * @param token the interface to implement
     * @param root  the root classpath directory to write the implementation source code to
     * @throws ImplerException if the specified {@code token} parameter is not an interface or cannot be implemented,
     *                         or the generated implementation cannot be written to the requested file
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (!token.isInterface()) {
            throw new ImplerException("Requested token is not an interface: " + token);
        }
        if (Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Cannot implement final objects: " + token);
        }
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Cannot implement private interface: " + token);
        }

        String implName = getImplName(token);
        try {
            String packageName = token.getPackageName();
            Path outputFile = getImplPath(token, root);
            Files.createDirectories(outputFile.getParent());
            try (BufferedWriter writer = new BufferedWriter(Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8))) {
                if (!packageName.equals("")) {
                    writer.write("package ");
                    writer.write(packageName);
                    writer.write(';');
                    writer.newLine();
                    writer.newLine();
                }

                writer.write("public class ");
                writer.write(implName);
                writer.write(" implements ");
                writer.write(getFullyQualifiedTypeName(token));
                writer.write(" {");
                writer.newLine();

                for (Method method : token.getMethods()) {
                    if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
                        continue;
                    }

                    writer.newLine();
                    writer.write("    @Override");
                    writer.newLine();

                    writer.write("    public ");
                    writer.write(getFullyQualifiedTypeName(method.getReturnType()));
                    writer.write(' ');
                    writer.write(method.getName());
                    writer.write('(');
                    Type[] paramTypes = method.getParameterTypes();
                    for (int i = 0; i < paramTypes.length; i++) {
                        writer.write(getFullyQualifiedTypeName(paramTypes[i]));
                        writer.write(" p" + i);
                        if (i < paramTypes.length - 1) {
                            writer.write(", ");
                        }
                    }
                    writer.write(") {");
                    writer.newLine();


                    if (method.getReturnType() != Void.TYPE) {
                        writer.write("        return ");
                        if (method.getReturnType().equals(boolean.class)) {
                            writer.write("false");
                        } else if (method.getReturnType().isPrimitive()) {
                            writer.write("0");
                        } else {
                            writer.write("null");
                        }
                        writer.write(";");
                        writer.newLine();
                    }
                    writer.write("    }");
                    writer.newLine();
                }
                writer.write("}");
                writer.newLine();
            } catch (IOException | UncheckedIOException | SecurityException e) {
                throw new ImplerException("Cannot write to file", e);
            }
        } catch (IOException | UnsupportedOperationException | InvalidPathException e) {
            throw new ImplerException("Cannot resolve output file", e);
        }
    }

    /**
     * Generates a class implementing the provided interface and saves it to a {@code JAR} file.
     * Implementation is generated the same way as in {@link #implement(Class, Path)}
     *
     * @param token   the interface to implement
     * @param jarFile the path to the {@code JAR} file to create
     * @throws ImplerException if the requested token cannot be implemented
     *                         (any exception that {@link #implement(Class, Path)} may throw,
     *                         or the error occurred during writing the target jar or any intermediate files
     *                         or the generated java code cannot be compiled
     *                         or the compiled classfile cannot be packed into a {@code JAR}
     * @see #implement(Class, Path)
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        try {
            Path srcPath = null;
            try {
                srcPath = Files.createTempDirectory("implementor-src");
                Path compilePath = null;
                try {
                    compilePath = Files.createTempDirectory("implementor-class");

                    implement(token, srcPath);
                    compile(srcPath, compilePath, getImplPath(token, srcPath), getContextClasspath(token));

                    packSingleJar(compilePath, jarFile, token);
                } catch (IOException e) {
                    throw new ImplerException("Cannot create temporary directory for generated class files", e);
                } finally {
                    cleanup(compilePath);
                }
            } catch (IOException e) {
                throw new ImplerException("Cannot create temporary directory for generated java files", e);
            } finally {
                cleanup(srcPath);
            }
        } catch (IOException e) {
            throw new ImplerException("Unable to remove temporary files", e);
        }
    }
}