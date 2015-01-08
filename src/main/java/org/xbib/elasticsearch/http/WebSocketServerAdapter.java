package org.xbib.elasticsearch.http;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.xbib.elasticsearch.websocket.Presence;

/**
 * WebSocket server adapter
 */
public interface WebSocketServerAdapter {

    /**
     * Emit a presence event.
     *
     * @param presence presence
     * @param topic topic
     * @param channel channel
     */
    void presence(Presence presence, String topic, Channel channel);

    /**
     * Emit a frame.
     *
     * @param handshaker handshaker
     * @param frame frame
     * @param context context
     */
    void frame(WebSocketServerHandshaker handshaker, WebSocketFrame frame, ChannelHandlerContext context);
}
