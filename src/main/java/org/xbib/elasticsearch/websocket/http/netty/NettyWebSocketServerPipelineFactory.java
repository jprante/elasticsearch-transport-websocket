
package org.xbib.elasticsearch.websocket.http.netty;

import org.jboss.netty.channel.ChannelPipeline;

/**
 * Netty implementation for a WebSocket server pipeline factory.
 * It is based on the HTTP server pipeline factory.
 */
public class NettyWebSocketServerPipelineFactory extends NettyHttpServerPipelineFactory {

    private final NettyWebSocketRequestHandler handler;
    
    public NettyWebSocketServerPipelineFactory(NettyWebSocketServerTransport transport) {
        super(transport);
        this.handler = new NettyWebSocketRequestHandler(transport);
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = super.getPipeline();
        pipeline.replace("handler", "handler", handler);
        return pipeline;
    }
}
