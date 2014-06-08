package org.xbib.elasticsearch.transport.netty;

import org.elasticsearch.Version;
import org.elasticsearch.common.compress.CompressorFactory;
import org.elasticsearch.common.io.stream.HandlesStreamOutput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.ThrowableObjectOutputStream;
import org.elasticsearch.common.lease.Releasables;
import org.elasticsearch.transport.NotSerializableTransportException;
import org.elasticsearch.transport.RemoteTransportException;
import org.elasticsearch.transport.TransportChannel;
import org.elasticsearch.transport.TransportResponse;
import org.elasticsearch.transport.TransportResponseOptions;
import org.elasticsearch.transport.support.TransportStatus;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.xbib.elasticsearch.common.bytes.ReleasableBytesReference;
import org.xbib.elasticsearch.common.io.stream.BytesStreamOutput;
import org.xbib.elasticsearch.common.io.stream.ReleasableBytesStreamOutput;
import org.xbib.elasticsearch.common.netty.ReleaseChannelFutureListener;

import java.io.IOException;
import java.io.NotSerializableException;

/**
 *
 */
public class NettyTransportChannel implements TransportChannel {

    private final NettyTransport transport;
    private final Version version;
    private final String action;
    private final Channel channel;
    private final long requestId;

    public NettyTransportChannel(NettyTransport transport, String action, Channel channel, long requestId, Version version) {
        this.version = version;
        this.transport = transport;
        this.action = action;
        this.channel = channel;
        this.requestId = requestId;
    }

    @Override
    public String action() {
        return this.action;
    }

    @Override
    public void sendResponse(TransportResponse response) throws IOException {
        sendResponse(response, TransportResponseOptions.EMPTY);
    }

    @Override
    public void sendResponse(TransportResponse response, TransportResponseOptions options) throws IOException {
        if (transport.compress) {
            options.withCompress(true);
        }
        byte status = 0;
        status = TransportStatus.setResponse(status);

        ReleasableBytesStreamOutput bStream = new ReleasableBytesStreamOutput(transport.bigArrays);
        boolean addedReleaseListener = false;
        try {
            bStream.skip(NettyHeader.HEADER_SIZE);
            StreamOutput stream = bStream;
            if (options.compress()) {
                status = TransportStatus.setCompress(status);
                stream = CompressorFactory.defaultCompressor().streamOutput(stream);
            }
            stream = new HandlesStreamOutput(stream);
            stream.setVersion(version);
            response.writeTo(stream);
            stream.close();

            ReleasableBytesReference bytes = bStream.ourBytes();
            ChannelBuffer buffer = bytes.toChannelBuffer();
            NettyHeader.writeHeader(buffer, requestId, status, version);
            ChannelFuture future = channel.write(buffer);
            ReleaseChannelFutureListener listener = new ReleaseChannelFutureListener(bytes);
            future.addListener(listener);
            addedReleaseListener = true;
        } finally {
            if (!addedReleaseListener) {
                Releasables.close(bStream.ourBytes());
            }
        }
    }

    @Override
    public void sendResponse(Throwable error) throws IOException {
        BytesStreamOutput stream = new BytesStreamOutput();
        try {
            stream.skip(NettyHeader.HEADER_SIZE);
            RemoteTransportException tx = new RemoteTransportException(transport.nodeName(), transport.wrapAddress(channel.getLocalAddress()), action, error);
            ThrowableObjectOutputStream too = new ThrowableObjectOutputStream(stream);
            too.writeObject(tx);
            too.close();
        } catch (NotSerializableException e) {
            stream.reset();
            stream.skip(org.elasticsearch.transport.netty.NettyHeader.HEADER_SIZE);
            RemoteTransportException tx = new RemoteTransportException(transport.nodeName(), transport.wrapAddress(channel.getLocalAddress()), action, new NotSerializableTransportException(error));
            ThrowableObjectOutputStream too = new ThrowableObjectOutputStream(stream);
            too.writeObject(tx);
            too.close();
        }

        byte status = 0;
        status = TransportStatus.setResponse(status);
        status = TransportStatus.setError(status);

        ChannelBuffer buffer = stream.ourBytes().toChannelBuffer();
        NettyHeader.writeHeader(buffer, requestId, status, version);
        channel.write(buffer);
    }

}
