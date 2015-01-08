package org.xbib.elasticsearch.common.bytes;

import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.io.stream.StreamInput;
import org.jboss.netty.buffer.ChannelBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.GatheringByteChannel;

/**
 * A reference to bytes for our Netty.
 */
public interface BytesReference {

    public static class Helper {

        public static boolean bytesEqual(BytesReference a, BytesReference b) {
            if (a == b) {
                return true;
            }
            if (a.length() != b.length()) {
                return false;
            }

            return bytesEquals(a, b);
        }

        // pkg-private for testing
        static boolean bytesEquals(BytesReference a, BytesReference b) {
            assert a.length() == b.length();
            for (int i = 0, end = a.length(); i < end; ++i) {
                if (a.get(i) != b.get(i)) {
                    return false;
                }
            }

            return true;
        }

        // pkg-private for testing
        static boolean slowBytesEquals(BytesReference a, BytesReference b) {
            assert a.length() == b.length();
            for (int i = 0, end = a.length(); i < end; ++i) {
                if (a.get(i) != b.get(i)) {
                    return false;
                }
            }

            return true;
        }

        public static int bytesHashCode(BytesReference a) {
            if (a.hasArray()) {
                return hashCode(a.array(), a.arrayOffset(), a.length());
            } else {
                return slowHashCode(a);
            }
        }

        // pkg-private for testing
        static int hashCode(byte[] array, int offset, int length) {
            int result = 1;
            for (int i = offset, end = offset + length; i < end; ++i) {
                result = 31 * result + array[i];
            }
            return result;
        }

        // pkg-private for testing
        static int slowHashCode(BytesReference a) {
            int result = 1;
            for (int i = 0, end = a.length(); i < end; ++i) {
                result = 31 * result + a.get(i);
            }
            return result;
        }
    }

    /**
     * Returns the byte at the specified index. Need to be between 0 and length.
     * @param index index
     * @return byte
     */
    byte get(int index);

    /**
     * The length.
     * @return length
     */
    int length();

    /**
     * Slice the bytes from the <tt>from</tt> index up to <tt>length</tt>.
     * @param from from
     * @param length length
     * @return byte reference
     */
    BytesReference slice(int from, int length);

    /**
     * A stream input of the bytes.
     * @return byte reference
     */
    StreamInput streamInput();

    /**
     * Writes the bytes directly to the output stream.
     * @param os output stream
     * @throws java.io.IOException if this method fails
     */
    void writeTo(OutputStream os) throws IOException;

    /**
     * Writes the bytes directly to the channel.
     * @param channel channel
     * @throws java.io.IOException if this method fails
     */
    void writeTo(GatheringByteChannel channel) throws IOException;

    /**
     * Returns the bytes as a single byte array.
     * @return bytes
     */
    byte[] toBytes();

    /**
     * Returns the bytes as a byte array, possibly sharing the underlying byte buffer.
     * @return byt earray
     */
    BytesArray toBytesArray();

    /**
     * Returns the bytes copied over as a byte array.
     * @return byte array
     */
    BytesArray copyBytesArray();

    /**
     * Returns the bytes as a channel buffer.
     * @return channel buffer
     */
    ChannelBuffer toChannelBuffer();

    /**
     * Is there an underlying byte array for this bytes reference.
     * @return true or false
     */
    boolean hasArray();

    /**
     * The underlying byte array (if exists).
     * @return byte array
     */
    byte[] array();

    /**
     * The offset into the underlying byte array.
     * @return offset
     */
    int arrayOffset();

    /**
     * Converts to a string based on utf8.
     * @return string
     */
    String toUtf8();

    /**
     * Converts to Lucene BytesRef.
     * @return bytes reference
     */
    BytesRef toBytesRef();

    /**
     * Converts to a copied Lucene BytesRef.
     * @return bytes reference
     */
    BytesRef copyBytesRef();
}
