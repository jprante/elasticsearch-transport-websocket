
package org.xbib.elasticsearch.websocket.common.bytes;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.base.Charsets;
import org.elasticsearch.common.io.stream.BytesStreamInput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

/**
 * BytesArray for our Netty.
 */
public class BytesArray implements BytesReference {

    private byte[] bytes;
    private int offset;
    private int length;

    public BytesArray(String bytes) {
        this(bytes.getBytes(Charsets.UTF_8));
    }

    public BytesArray(byte[] bytes) {
        this.bytes = bytes;
        this.offset = 0;
        this.length = bytes.length;
    }

    public BytesArray(byte[] bytes, int offset, int length) {
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public byte get(int index) {
        return bytes[offset + index];
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public BytesReference slice(int from, int length) {
        if (from < 0 || (from + length) > this.length) {
            throw new ElasticsearchIllegalArgumentException("can't slice a buffer with length [" + this.length + "], with slice parameters from [" + from + "], length [" + length + "]");
        }
        return new BytesArray(bytes, offset + from, length);
    }

    @Override
    public StreamInput streamInput() {
        return new BytesStreamInput(bytes, offset, length, false);
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        os.write(bytes, offset, length);
    }

    @Override
    public byte[] toBytes() {
        if (offset == 0 && bytes.length == length) {
            return bytes;
        }
        return Arrays.copyOfRange(bytes, offset, offset + length);
    }

    @Override
    public BytesArray toBytesArray() {
        return this;
    }

    @Override
    public BytesArray copyBytesArray() {
        return new BytesArray(Arrays.copyOfRange(bytes, offset, offset + length));
    }

    @Override
    public ChannelBuffer toChannelBuffer() {
        return ChannelBuffers.wrappedBuffer(bytes, offset, length);
    }

    @Override
    public boolean hasArray() {
        return true;
    }

    @Override
    public byte[] array() {
        return bytes;
    }

    @Override
    public int arrayOffset() {
        return offset;
    }

    @Override
    public String toUtf8() {
        if (length == 0) {
            return "";
        }
        return new String(bytes, offset, length, Charsets.UTF_8);
    }

    @Override
    public boolean equals(Object obj) {
        return bytesEquals((BytesArray) obj);
    }

    public boolean bytesEquals(BytesArray other) {
        if (length == other.length) {
            int otherUpto = other.offset;
            final byte[] otherBytes = other.bytes;
            final int end = offset + length;
            for (int upto = offset; upto < end; upto++, otherUpto++) {
                if (bytes[upto] != otherBytes[otherUpto]) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = 0;
        final int end = offset + length;
        for (int i = offset; i < end; i++) {
            result = 31 * result + bytes[i];
        }
        return result;
    }
}