
package org.xbib.elasticsearch.websocket.common.bytes;

import java.io.IOException;
import java.io.OutputStream;

import org.elasticsearch.common.io.stream.StreamInput;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * A reference to bytes for our Netty.
 */
public interface BytesReference {

    /**
     * Returns the byte at the specified index. Need to be between 0 and length.
     */
    byte get(int index);

    /**
     * The length.
     */
    int length();

    /**
     * Slice the bytes from the <tt>from</tt> index up to <tt>length</tt>.
     */
    BytesReference slice(int from, int length);

    /**
     * A stream input of the bytes.
     */
    StreamInput streamInput();

    /**
     * Writes the bytes directly to the output stream.
     */
    void writeTo(OutputStream os) throws IOException;

    /**
     * Returns the bytes as a single byte array.
     */
    byte[] toBytes();

    /**
     * Returns the bytes as a byte array, possibly sharing the underlying byte buffer.
     */
    BytesArray toBytesArray();

    /**
     * Returns the bytes copied over as a byte array.
     */
    BytesArray copyBytesArray();

    /**
     * Returns the bytes as a channel buffer.
     */
    ChannelBuffer toChannelBuffer();

    /**
     * Is there an underlying byte array for this bytes reference.
     */
    boolean hasArray();

    /**
     * The underlying byte array (if exists).
     */
    byte[] array();

    /**
     * The offset into the underlying byte array.
     */
    int arrayOffset();

    /**
     * Converts to a string based on utf8.
     */
    String toUtf8();
}
