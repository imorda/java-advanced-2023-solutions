package info.kgeorgiy.ja.belousov.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.Executors;

/**
 * Implementation of a UDP server that echoes any request adding a prefix "Hello, " to it
 */
public class HelloUDPNonblockingServer extends AbstractHelloUDPServer {
    final private Deque<ByteBuffer> readBufs = new ArrayDeque<>();
    final private Deque<DatagramChannelPacket> writePackets = new ArrayDeque<>();

    private Selector selector;
    private DatagramChannel channel;

    /**
     * Main function used as entrypoint when launched as a standalone application
     *
     * @param args Required:
     *             - Server port number
     *             - Number of worker threads
     */
    public static void main(String[] args) {
        try (AbstractHelloUDPServer instance = new HelloUDPNonblockingServer()) {
            instance.mainImpl(args);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param port    server port.
     * @param threads number of working threads.
     */
    @Override
    public void start(int port, int threads) {
        super.start(port, threads);

        try {
            selector = Selector.open();
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            channel.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            System.err.println("Error initializing a server: " + e.getMessage());
            return;
        }

        for (int i = 0; i < threads; i++) {
            try {
                readBufs.add(ByteBuffer.allocate(channel.socket().getReceiveBufferSize()));
            } catch (SocketException e) {
                System.err.println("Socket is broken: " + e.getMessage());
                return;
            }
        }

        workers = Executors.newFixedThreadPool(threads);
        serverThread = new Thread(() -> {
            while (channel.isOpen() && !Thread.interrupted()) {
                try {
                    selector.select();

                    for (Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator(); keyIterator.hasNext(); ) {
                        SelectionKey key = keyIterator.next();
                        try {
                            DatagramChannel channel = ((DatagramChannel) key.channel());
                            if (key.isReadable()) {
                                ByteBuffer buffer = readBufs.remove();

                                if (readBufs.isEmpty()) {
                                    key.interestOpsAnd(~SelectionKey.OP_READ);
                                }

                                SocketAddress address = channel.receive(buffer.clear());
                                String requestText = NonblockingUtils.decodePacket(buffer);

                                workers.submit(() -> {
                                    String responseText = getResponse(requestText);

                                    DatagramChannelPacket response = new DatagramChannelPacket(address,
                                            buffer.clear().put(responseText.getBytes()).flip());

                                    synchronized (writePackets) {
                                        writePackets.add(response);
                                        if ((key.interestOps() & SelectionKey.OP_WRITE) == 0) {
                                            key.interestOpsOr(SelectionKey.OP_WRITE);
                                            selector.wakeup();
                                        }
                                    }
                                });
                            } else if (key.isWritable()) {
                                DatagramChannelPacket packet;
                                synchronized (writePackets) {
                                    packet = writePackets.remove();
                                    if (writePackets.isEmpty()) {
                                        key.interestOpsAnd(~SelectionKey.OP_WRITE);
                                    }
                                }

                                channel.send(packet.byteBuffer(), packet.address());
                                readBufs.add(packet.byteBuffer());
                                key.interestOpsOr(SelectionKey.OP_READ);
                            }
                        } finally {
                            keyIterator.remove();
                        }
                    }
                } catch (ClosedChannelException | ClosedSelectorException ignored) {
                } catch (IOException e) {
                    System.err.printf("IO error in server channel: %s%n", e.getMessage());
                }
            }
        });
        serverThread.start();
    }

    @Override
    public void close() {
        super.close();
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                System.err.println("Error closing a channel: " + e.getMessage());
            }
        }
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                System.err.println("Error closing selector: " + e.getMessage());
            }
        }
    }

    private record DatagramChannelPacket(SocketAddress address, ByteBuffer byteBuffer) {
    }
}
