package info.kgeorgiy.ja.belousov.walk;

import java.io.IOException;

@FunctionalInterface
public interface IOFunction<T, R> {
    R apply(T param) throws IOException;
}
