
package org.xbib.elasticsearch.websocket.transport.netty;

import org.elasticsearch.Version;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 */
public class NettyHeader {

    public static final int HEADER_SIZE = 2 + 4 + 8 + 1 + 4;

    public static void writeHeader(ChannelBuffer buffer, long requestId, byte status, Version version) {
        int index = buffer.readerIndex();
        buffer.setByte(index, 'E');
        index += 1;
        buffer.setByte(index, 'S');
        index += 1;
        // write the size, the size indicates the remaining message size, not including the size int
        buffer.setInt(index, buffer.readableBytes() - 6);
        index += 4;
        buffer.setLong(index, requestId);
        index += 8;
        buffer.setByte(index, status);
        index += 1;
        buffer.setInt(index, version.id);
    }
}
