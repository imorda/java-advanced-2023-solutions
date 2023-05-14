package info.kgeorgiy.ja.belousov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of a UDP client that concurrently sends requests in format
 * "xxxyy_zz" where xxx - a given string, yy - number of a sender thread, zz - number of a request in a thread
 */
public class HelloUDPClient implements HelloClient {
    private static final int SOCKET_READ_TIMEOUT_MILLIS = 200;

    /**
     * Main function used as entrypoint when launched as a standalone application
     *
     * @param args Required:
     *             - Hostname (or ip address) of a server
     *             - Server port number
     *             - Requests prefix (xxx)
     *             - Number of requests threads
     *             - Number of requests inside each thread
     */
    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("Usage: java HelloUDPClient <hostname> <port> <request> <threads> <requests>");
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

    /**
     * {@inheritDoc}
     *
     * @param host     server host
     * @param port     server port
     * @param prefix   request prefix
     * @param threads  number of request threads
     * @param requests number of requests per thread.
     * @throws IllegalArgumentException when port is out of range
     */
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        SocketAddress address = new InetSocketAddress(host, port);

        List<Thread> threadInstances = new ArrayList<>(threads);
        for (int thread = 0; thread < threads; thread++) {
            final int threadFinal = thread;
            threadInstances.add(new Thread(() -> {
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(SOCKET_READ_TIMEOUT_MILLIS);

                    for (int request = 0; request < requests; ++request) {
                        String requestText = String.format("%s%d_%d", prefix, threadFinal + 1, request + 1);
                        DatagramPacket packet = new DatagramPacket(requestText.getBytes(), requestText.length(), address);

                        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                            try {
                                socket.send(packet);
                            } catch (IOException | IllegalArgumentException e) {
                                System.err.format("Error sending packet: %s%n", e.getMessage());
                            }

                            byte[] buffer = new byte[socket.getReceiveBufferSize()];
                            DatagramPacket response = new DatagramPacket(buffer, buffer.length);

                            try {
                                socket.receive(response);
                            } catch (SocketTimeoutException ignored) {
                                continue;
                            } catch (IOException e) {
                                System.err.format("Error receiving packet: %s%n", e.getMessage());
                            }

                            if (response.getLength() > 0) {
                                String expectedResponse = HelloUDPServer.RESPONSE_PREFIX + requestText;
                                String responseText = new String(response.getData(), response.getOffset(), response.getLength());

                                if (!expectedResponse.equals(responseText)) {
                                    printReport(System.err, requestText, responseText);
                                    continue;
                                }
                                printReport(System.out, requestText, responseText);
                                break;
                            }
                        }
                        if (Thread.interrupted()) {
                            break;
                        }
                    }
                } catch (SocketException e) {
                    System.err.printf("Can't open socket in thread %d: %s", threadFinal, e);
                }
            }));
        }

        threadInstances.forEach(Thread::start);

        for (int i = 0; i < threads; i++) {
            try {
                threadInstances.get(i).join();
            } catch (InterruptedException ignored) { // Main thread was interrupted
                i--; // Try again from the same thread
            }
        }
    }

    private void printReport(final PrintStream stream, final String request, final String response) {
        stream.printf("> \"%s\"%n  < \"%s\"%n", request, response);
    }
}

