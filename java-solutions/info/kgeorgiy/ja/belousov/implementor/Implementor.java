package info.kgeorgiy.ja.belousov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Implementor implements Impler {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java Implementor <className> <outputDir>");
            return;
        }
        String className = args[0];
        String outputDir = args[1];
        try {
            staticImplement(Class.forName(className), Paths.get(outputDir));
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

    private static String getFullyQualifiedTypeName(Type type) {
        return type.getTypeName().replace('$', '.');
    }

    public static void staticImplement(Class<?> token, Path root) throws ImplerException {
        if (!token.isInterface()) {
            throw new ImplerException("Requested token is not an interface: " + token);
        }
        if (Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Cannot implement final objects: " + token);
        }
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Cannot implement private interface: " + token);
        }

        String implName = token.getSimpleName() + "Impl";

        try {
            Path outputFile = Files.createDirectories(root
                            .resolve(token.getPackageName().replace(".", File.separator)))
                    .resolve(implName + ".java");
            try (BufferedWriter writer = new BufferedWriter(Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8))) {
                writer.write("package ");
                writer.write(token.getPackage().getName());
                writer.write(';');
                writer.write(System.lineSeparator());
                writer.write(System.lineSeparator());

                writer.write("public class ");
                writer.write(implName);
                writer.write(" implements ");
                writer.write(getFullyQualifiedTypeName(token));
                writer.write(" {");
                writer.write(System.lineSeparator());

                for (Method method : token.getMethods()) {
                    if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
                        continue;
                    }

                    writer.write(System.lineSeparator());
                    writer.write("    @Override");
                    writer.write(System.lineSeparator());

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
                    writer.write(System.lineSeparator());


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
                        writer.write(System.lineSeparator());
                    }
                    writer.write("    }");
                    writer.write(System.lineSeparator());
                }
                writer.write("}");
                writer.write(System.lineSeparator());
            } catch (IOException | UncheckedIOException | SecurityException e) {
                throw new ImplerException("Cannot write to file", e);
            }
        } catch (IOException | UnsupportedOperationException | InvalidPathException e) {
            throw new ImplerException("Cannot resolve output file", e);
        }
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        staticImplement(token, root);
    }
}