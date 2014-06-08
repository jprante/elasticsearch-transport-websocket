package org.xbib.elasticsearch.http.netty.client;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.xbib.elasticsearch.websocket.client.WebSocketActionListener;
import org.xbib.elasticsearch.websocket.client.WebSocketClient;
import org.xbib.elasticsearch.websocket.client.WebSocketClientBulkRequest;
import org.xbib.elasticsearch.websocket.client.WebSocketClientFactory;
import org.xbib.elasticsearch.websocket.client.WebSocketClientRequest;
import org.xbib.elasticsearch.http.netty.NettyInteractiveRequest;

import java.net.URI;
import java.util.concurrent.Executors;

/**
 * A factory for creating Websocket clients. The entry point for creating and
 * connecting a client. Can and should be used to create multiple instances.
 * <p/>
 * Extended for Websocket client request methods.
 */
public class NettyWebSocketClientFactory implements WebSocketClientFactory {

    private NioClientSocketChannelFactory socketChannelFactory = new NioClientSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool());

    /**
     * Create a new WebSocket client
     *
     * @param url      URL to connect to.
     * @param listener Callback interface to receive events
     * @return A WebSocket client. Call {@link NettyWebSocketClient#connect()} to
     * connect.
     */
    @Override
    public WebSocketClient newClient(final URI url, final WebSocketActionListener listener) {
        ClientBootstrap bootstrap = new ClientBootstrap(socketChannelFactory);

        String protocol = url.getScheme();
        if (!protocol.equals("ws") && !protocol.equals("wss")) {
            throw new IllegalArgumentException("unsupported protocol: " + protocol);
        }

        final NettyWebSocketClientHandler clientHandler = new NettyWebSocketClientHandler(bootstrap, url, listener);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("decoder", new HttpResponseDecoder());
                pipeline.addLast("encoder", new HttpRequestEncoder());
                pipeline.addLast("ws-handler", clientHandler);
                return pipeline;
            }
        });

        return clientHandler;
    }

    @Override
    public void shutdown() {
        socketChannelFactory.releaseExternalResources();
    }

    @Override
    public WebSocketClientRequest newRequest() {
        return new NettyInteractiveRequest();
    }

    @Override
    public WebSocketClientBulkRequest indexRequest() {
        return new NettyWebSocketBulkRequest("index");
    }

    @Override
    public WebSocketClientBulkRequest deleteRequest() {
        return new NettyWebSocketBulkRequest("delete");
    }

    @Override
    public WebSocketClientRequest flushRequest() {
        return new NettyInteractiveRequest().type("flush");
    }
}