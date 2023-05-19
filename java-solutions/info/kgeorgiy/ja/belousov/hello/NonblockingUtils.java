package info.kgeorgiy.ja.belousov.hello;

import java.nio.ByteBuffer;

/**
 * Utility class for common procedures in java nonblocking io
 */
public class NonblockingUtils {
    /**
     * Decodes a packet inside a {@link ByteBuffer} obtained from a socket into a string
     *
     * @param buffer buffer with the packet itself
     * @return the decoded string value of the packet
     */
    public static String decodePacket(ByteBuffer buffer) {
        byte[] requestBytes = new byte[buffer.flip().remaining()];
        buffer.get(requestBytes);
        return new String(requestBytes);
    }
}
