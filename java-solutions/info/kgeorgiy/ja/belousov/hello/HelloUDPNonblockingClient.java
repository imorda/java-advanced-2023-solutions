package info.kgeorgiy.ja.belousov.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of a UDP client that concurrently sends requests in format
 * "xxxyy_zz" where xxx - a given string, yy - number of a sender thread, zz - number of a request in a thread
 */
public class HelloUDPNonblockingClient extends AbstractHelloUDPClient {
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
        new HelloUDPNonblockingClient().mainImpl(args);
    }

    /**
     * {@inheritDoc}
     *
     * @param host     server host
     * @param port     server port
     * @param prefix   request prefix
     * @param sockets  number of request threads
     * @param requests number of requests per thread.
     */
    @Override
    public void run(String host, int port, String prefix, int sockets, int requests) {
        SocketAddress address = new InetSocketAddress(host, port);

        List<Channel> channelInstances = new ArrayList<>(sockets);
        try (Selector selector = Selector.open()) {
            for (int channel = 0; channel < sockets; channel++) {
                try {
                    DatagramChannel datagramChannel = DatagramChannel.open();
                    datagramChannel.configureBlocking(false);
                    datagramChannel.register(selector, SelectionKey.OP_WRITE,
                            new ChannelData(channel, requests,
                                    ByteBuffer.allocate(datagramChannel.socket().getReceiveBufferSize())));
                    datagramChannel.connect(address);
                    channelInstances.add(datagramChannel);
                } catch (IOException e) {
                    System.err.format("Error opening a DatagramChannel %d.%n", channel);
                    return;
                }
            }

            while (!Thread.interrupted() && !selector.keys().isEmpty()) {
                int selectedCount = selector.select(SOCKET_READ_TIMEOUT_MILLIS); // If READ doesn't happen in time, we repeat write branch.

                if (selectedCount == 0) { // All tasks are "read" and hung
                    for (SelectionKey key : selector.keys()) {
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                    continue;
                }

                for (Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator(); keyIterator.hasNext(); ) {
                    SelectionKey key = keyIterator.next();
                    try {
                        ChannelData attachment = (ChannelData) key.attachment();
                        DatagramChannel channel = ((DatagramChannel) key.channel());
                        ByteBuffer buffer = attachment.getBuffer();
                        if (key.isReadable()) {
                            channel.receive(buffer.clear());
                            String responseText = NonblockingUtils.decodePacket(buffer);

                            if (proccessResponse(responseText, prefix, attachment.getPos(), attachment.getRequestCounter())) {
                                attachment.incrementRequestCounter();

                                if (attachment.getRequestCounter() >= attachment.getTotalRequests()) {
                                    channel.close();
                                    key.cancel();
                                    continue;
                                }
                            }
                            key.interestOps(SelectionKey.OP_WRITE);
                        } else if (key.isWritable()) {
                            buffer.clear().put(getRequest(prefix, attachment.getPos(), attachment.getRequestCounter()).getBytes()).flip();
                            channel.send(buffer, address);
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    } finally {
                        keyIterator.remove();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Some of sockets are dead");
        } finally {
            for (Channel channel : channelInstances) {
                try {
                    channel.close(); // Only has effect on failure. On success, all channels are closed already.
                } catch (IOException e) {
                    System.err.println("Error closing a channel");
                }
            }
        }
    }

    /**
     * Boilerplate driven record-like (but with a mutable field) data class
     * Java equals boilerplate
     * Boilerplate equals java
     * <3
     */
    private static class ChannelData {
        private final int pos;
        private final int totalRequests;
        private final ByteBuffer buffer;
        private int requestCounter = 0;

        ChannelData(int pos, int totalRequests, ByteBuffer buffer) {
            this.pos = pos;
            this.totalRequests = totalRequests;
            this.buffer = buffer;
        }

        void incrementRequestCounter() {
            this.requestCounter++;
        }

        int getPos() {
            return pos;
        }

        int getRequestCounter() {
            return requestCounter;
        }

        int getTotalRequests() {
            return totalRequests;
        }

        ByteBuffer getBuffer() {
            return buffer;
        }
    }
}