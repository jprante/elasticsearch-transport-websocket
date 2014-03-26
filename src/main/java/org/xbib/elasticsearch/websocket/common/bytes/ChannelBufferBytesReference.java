
package org.xbib.elasticsearch.websocket.common.bytes;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.GatheringByteChannel;

import org.apache.lucene.util.BytesRef;

import org.elasticsearch.common.base.Charsets;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;

import org.jboss.netty.buffer.ChannelBuffer;
import org.xbib.elasticsearch.websocket.transport.netty.ChannelBufferStreamInputFactory;

/**
 *
 * ChannelBufferBytesReference for our Netty
 */
public class ChannelBufferBytesReference implements BytesReference {

    private final ChannelBuffer buffer;

    public ChannelBufferBytesReference(ChannelBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public byte get(int index) {
        return buffer.getByte(buffer.readerIndex() + index);
    }

    @Override
    public int length() {
        return buffer.readableBytes();
    }

    @Override
    public BytesReference slice(int from, int length) {
        return new ChannelBufferBytesReference(buffer.slice(from, length));
    }

    @Override
    public StreamInput streamInput() {
        return ChannelBufferStreamInputFactory.create(buffer.duplicate());
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        buffer.getBytes(buffer.readerIndex(), os, length());
    }

    @Override
    public void writeTo(GatheringByteChannel channel) throws IOException {
        buffer.getBytes(buffer.readerIndex(), channel, length());
    }

    @Override
    public byte[] toBytes() {
        return copyBytesArray().toBytes();
    }

    @Override
    public BytesArray toBytesArray() {
        if (buffer.hasArray()) {
            return new BytesArray(buffer.array(), buffer.arrayOffset() + buffer.readerIndex(), buffer.readableBytes());
        }
        return copyBytesArray();
    }

    @Override
    public BytesArray copyBytesArray() {
        byte[] copy = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), copy);
        return new BytesArray(copy);
    }


    @Override
    public boolean hasArray() {
        return buffer.hasArray();
    }

    @Override
    public byte[] array() {
        return buffer.array();
    }

    @Override
    public int arrayOffset() {
        return buffer.arrayOffset() + buffer.readerIndex();
    }

    @Override
    public String toUtf8() {
        return buffer.toString(Charsets.UTF_8);
    }

    @Override
    public BytesRef toBytesRef() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public BytesRef copyBytesRef() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public org.elasticsearch.common.netty.buffer.ChannelBuffer toChannelBuffer() {
        //  return buffer.duplicate();
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int hashCode() {
        return Helper.bytesHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return Helper.bytesEqual(this, (BytesReference) obj);
    }
}
