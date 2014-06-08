package org.xbib.elasticsearch.http.netty;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.xbib.elasticsearch.websocket.Presence;

/**
 * Handles HTTP request and upgrades HTTP to WebSocket if appropriate.
 */
@ChannelHandler.Sharable
public class NettyHttpRequestHandler extends SimpleChannelUpstreamHandler {

    protected final NettyWebSocketServerTransport serverTransport;

    protected WebSocketServerHandshaker handshaker;

    private static String getWebSocketLocation(HttpRequest req) {
        return "ws://" + req.headers().get(HttpHeaders.Names.HOST) + WEBSOCKET_PATH;
    }

    private static final String WEBSOCKET_PATH = "/websocket";

    public NettyHttpRequestHandler(NettyWebSocketServerTransport serverTransport) {
        this.serverTransport = serverTransport;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        HttpRequest request = (HttpRequest) e.getMessage();
        if (request.getUri().startsWith(WEBSOCKET_PATH)) {
            // Websocket handshake
            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(request), null, false);
            handshaker = wsFactory.newHandshaker(request);
            if (handshaker == null) {
                wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
            } else {
                handshaker.handshake(ctx.getChannel(), request).addListener(WebSocketServerHandshaker.HANDSHAKE_LISTENER);
            }
            // extract topic from request URI
            String topic = request.getUri();
            if (topic.length() > WEBSOCKET_PATH.length() + 1) {
                topic = topic.substring(WEBSOCKET_PATH.length() + 1);
                serverTransport.presence(Presence.CONNECTED, topic, e.getChannel());
            }
        } else {
            NettyHttpRequest nettyHttpRequest =new NettyHttpRequest(request);
            serverTransport.dispatchRequest(nettyHttpRequest, new NettyHttpChannel(serverTransport, e.getChannel(), nettyHttpRequest));
            super.messageReceived(ctx, e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        serverTransport.exceptionCaught(ctx, e);
        e.getChannel().close();
    }

}