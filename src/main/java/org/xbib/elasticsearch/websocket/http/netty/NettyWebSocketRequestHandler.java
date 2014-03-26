
package org.xbib.elasticsearch.websocket.http.netty;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * Netty implementation for a WebSocket request handler.
 * It is based on the HTTP request handler.
 */
public class NettyWebSocketRequestHandler extends NettyHttpRequestHandler {

    public NettyWebSocketRequestHandler(NettyWebSocketServerTransport serverTransport) {
        super(serverTransport);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            super.messageReceived(ctx, e);
        } else if (msg instanceof WebSocketFrame) {
            serverTransport.frame(handshaker, (WebSocketFrame) msg, ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        serverTransport.exceptionCaught(ctx, e);
        e.getChannel().close();
    }

}