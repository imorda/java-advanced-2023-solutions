package info.kgeorgiy.ja.belousov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.PrintStream;
import java.util.Objects;

public abstract class AbstractHelloUDPClient implements HelloClient {
    protected static final int SOCKET_READ_TIMEOUT_MILLIS = 200;

    protected void mainImpl(String[] args) {
        if (args == null || args.length != 5) {
            System.err.format("Usage: java %s <hostname> <port> <request> <threads> <requests>", this.getClass().getName());
            return;
        }

        String hostname = args[0];
        int port;
        String request = args[2];
        int threads;
        int requests;
        try {
            Objects.requireNonNull(hostname);
            Objects.requireNonNull(request);

            port = Integer.parseInt(args[1]);
            threads = Integer.parseInt(args[3]);
            requests = Integer.parseInt(args[4]);
        } catch (NumberFormatException | NullPointerException e) {
            System.err.println("Incorrect args format!");
            return;
        }

        HelloUDPClient instance = new HelloUDPClient();
        try {
            instance.run(hostname, port, request, threads, requests);
        } catch (IllegalArgumentException e) {
            System.err.printf("Incorrect port number: %s%n", e.getMessage());
        }
    }

    protected void printReport(final PrintStream stream, final String request, final String response) {
        stream.printf("> \"%s\"%n  < \"%s\"%n", request, response);
    }

    protected String getRequest(String prefix, int pos, int requestNumber) {
        return String.format("%s%d_%d", prefix, pos + 1, requestNumber + 1);
    }

    protected boolean proccessResponse(String response, String prefix, int pos, int requestNumber) {
        if (response.length() > 0) {
            String request = getRequest(prefix, pos, requestNumber);
            String expectedResponse = AbstractHelloUDPServer.getResponse(request);

            if (!expectedResponse.equals(response)) {
                printReport(System.err, request, response);
                return false;
            }
            printReport(System.out, request, response);
            return true;
        }
        return false;
    }
}
