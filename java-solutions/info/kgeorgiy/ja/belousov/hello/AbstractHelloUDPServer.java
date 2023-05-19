package info.kgeorgiy.ja.belousov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractHelloUDPServer implements HelloServer {
    private static final String RESPONSE_PREFIX = "Hello, ";
    protected Thread serverThread = null;
    protected ExecutorService workers = null;

    protected static String getResponse(String request) {
        return RESPONSE_PREFIX + request;
    }

    protected void mainImpl(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Usage: java HelloUDPServer <port> <threads>");
            return;
        }

        int port;
        int threads;
        try {
            port = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
        } catch (NumberFormatException | NullPointerException e) {
            System.err.println("Incorrect args format!");
            return;
        }

        try {
            start(port, threads);
        } catch (IllegalArgumentException e) {
            System.err.printf("Incorrect port number: %s%n", e.getMessage());
        } catch (IllegalStateException e) {
            System.err.printf("Cannot bind socket: %s%n", e.getMessage());
        }

        try {
            System.in.read();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void start(int port, int threads) {
        close();
        workers = Executors.newFixedThreadPool(threads);
    }

    @Override
    public void close() {
        if (serverThread != null) {
            serverThread.interrupt();
        }
        if (workers != null) {
            workers.shutdownNow();
        }
    }
}
