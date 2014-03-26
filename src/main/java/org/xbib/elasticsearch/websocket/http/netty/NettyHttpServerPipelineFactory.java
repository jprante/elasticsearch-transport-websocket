
package org.xbib.elasticsearch.websocket.http.netty;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

public class NettyHttpServerPipelineFactory implements ChannelPipelineFactory {

    protected final NettyWebSocketServerTransport transport;

    private final NettyHttpRequestHandler requestHandler;

    NettyHttpServerPipelineFactory(NettyWebSocketServerTransport transport) {
        this.transport = transport;
        this.requestHandler = new NettyHttpRequestHandler(transport);
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("openChannels", transport.serverOpenChannels);
        HttpRequestDecoder requestDecoder = new HttpRequestDecoder(
                (int) transport.maxInitialLineLength.bytes(),
                (int) transport.maxHeaderSize.bytes(),
                (int) transport.maxChunkSize.bytes());
        if (transport.maxCumulationBufferCapacity != null) {
            if (transport.maxCumulationBufferCapacity.bytes() > Integer.MAX_VALUE) {
                requestDecoder.setMaxCumulationBufferCapacity(Integer.MAX_VALUE);
            } else {
                requestDecoder.setMaxCumulationBufferCapacity((int) transport.maxCumulationBufferCapacity.bytes());
            }
        }
        if (transport.maxCompositeBufferComponents != -1) {
            requestDecoder.setMaxCumulationBufferComponents(transport.maxCompositeBufferComponents);
        }
        pipeline.addLast("decoder", requestDecoder);
        if (transport.compression) {
            pipeline.addLast("decoder_compress", new HttpContentDecompressor());
        }
        HttpChunkAggregator httpChunkAggregator = new HttpChunkAggregator((int) transport.maxContentLength.bytes());
        if (transport.maxCompositeBufferComponents != -1) {
            httpChunkAggregator.setMaxCumulationBufferComponents(transport.maxCompositeBufferComponents);
        }
        pipeline.addLast("aggregator", httpChunkAggregator);
        pipeline.addLast("encoder", new HttpResponseEncoder());
        if (transport.compression) {
            pipeline.addLast("encoder_compress", new HttpContentCompressor(transport.compressionLevel));
        }
        pipeline.addLast("handler", requestHandler);
        return pipeline;
    }
}
