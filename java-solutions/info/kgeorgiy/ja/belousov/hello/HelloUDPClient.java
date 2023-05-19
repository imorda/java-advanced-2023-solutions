package info.kgeorgiy.ja.belousov.hello;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of a UDP client that concurrently sends requests in format
 * "xxxyy_zz" where xxx - a given string, yy - number of a sender thread, zz - number of a request in a thread
 */
public class HelloUDPClient extends AbstractHelloUDPClient {
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
        new HelloUDPClient().mainImpl(args);
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
                        String requestText = getRequest(prefix, threadFinal, request);
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

                            String responseText = new String(response.getData(), response.getOffset(), response.getLength());
                            if (proccessResponse(responseText, prefix, threadFinal, request)) {
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
}

