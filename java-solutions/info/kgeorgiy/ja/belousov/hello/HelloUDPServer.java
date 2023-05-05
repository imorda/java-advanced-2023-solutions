package info.kgeorgiy.ja.belousov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * Implementation of a UDP server that echoes any request adding a prefix "Hello, " to it
 */
public class HelloUDPServer implements HelloServer {
    static final String RESPONSE_PREFIX = "Hello, ";

    private DatagramSocket socket = null;
    private Thread serverThread = null;
    private ExecutorService workers = null;

    /**
     * Main function used as entrypoint when launched as a standalone application
     *
     * @param args Required:
     *             - Server port number
     *             - Number of worker threads
     */
    public static void main(String[] args) {
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

        HelloUDPServer instance = new HelloUDPServer();
        try {
            instance.start(port, threads);
        } catch (IllegalArgumentException e) {
            System.err.printf("Incorrect port number: %s%n", e.getMessage());
        } catch (IllegalStateException e) {
            System.err.printf("Cannot bind socket: %s%n", e.getMessage());
        }

        try {
            System.in.read();
        } catch (IOException ignored) {
        }

        instance.close();
    }

    /**
     * {@inheritDoc}
     *
     * @param port    server port.
     * @param threads number of working threads.
     * @throws IllegalStateException    if the socket can't be bound
     * @throws IllegalArgumentException if port is out of range
     */
    @Override
    public void start(int port, int threads) {
        close();

        Semaphore availableResource = new Semaphore(threads);
        workers = Executors.newFixedThreadPool(threads);
        try {
            socket = new DatagramSocket(port);
            serverThread = new Thread(() -> {
                try {
                    while (!socket.isClosed() && !Thread.interrupted()) {
                        try {
                            DatagramPacket packet = new DatagramPacket(
                                    new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());

                            socket.receive(packet);

                            availableResource.acquire();
                            try {
                                workers.submit(() -> {
                                    try {
                                        String packetText = new String(packet.getData(), packet.getOffset(), packet.getLength());
                                        String responseText = RESPONSE_PREFIX + packetText;

                                        DatagramPacket response = new DatagramPacket(responseText.getBytes(),
                                                responseText.length(), packet.getSocketAddress());

                                        try {
                                            socket.send(response);
                                        } catch (IOException | IllegalArgumentException e) {
                                            System.err.format("Error sending packet: %s%n", e.getMessage());
                                        }
                                    } finally {
                                        availableResource.release();
                                    }
                                });
                            } catch (RejectedExecutionException ignored) {
                                availableResource.release();
                            }
                        } catch (SocketException e) {
                            if (!socket.isClosed()) {
                                System.err.printf("Server UDP error: %s%n", e.getMessage());
                            }
                        } catch (IOException e) {
                            System.err.printf("IO error in server socket: %s%n", e.getMessage());
                        }
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    Thread.currentThread().interrupt();
                }
            });
            serverThread.start();
        } catch (SocketException e) {
            throw new IllegalStateException("Error opening socket", e);
        }
    }

    @Override
    public void close() {
        if (socket != null) {
            socket.close();
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
        if (workers != null) {
            workers.shutdownNow();
        }
    }
}
