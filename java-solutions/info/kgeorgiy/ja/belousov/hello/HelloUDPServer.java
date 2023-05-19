package info.kgeorgiy.ja.belousov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * Implementation of a UDP server that echoes any request adding a prefix "Hello, " to it
 */
public class HelloUDPServer extends AbstractHelloUDPServer {
    private DatagramSocket socket = null;


    /**
     * Main function used as entrypoint when launched as a standalone application
     *
     * @param args Required:
     *             - Server port number
     *             - Number of worker threads
     */
    public static void main(String[] args) {
        try (AbstractHelloUDPServer instance = new HelloUDPServer()) {
            instance.mainImpl(args);
        }
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
        super.start(port, threads);
        Semaphore availableResource = new Semaphore(threads);
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
                                        String responseText = getResponse(packetText);

                                        DatagramPacket response = new DatagramPacket(responseText.getBytes(),
                                                responseText.length(), packet.getSocketAddress());

                                        try {
                                            socket.send(response);
                                        } catch (IOException | IllegalArgumentException e) {
                                            if (!socket.isClosed()) {
                                                System.err.format("Error sending packet: %s%n", e.getMessage());
                                            }
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
        super.close();
        if (socket != null) {
            socket.close();
        }
    }
}
