
package org.xbib.elasticsearch.websocket.client;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * A WebSocket client.
 */
public interface WebSocketClient<F extends ChannelFuture, Frame extends WebSocketFrame> {

    /**
     * The channel this client has opened.
     * @return the channel
     */
    Channel channel();
    
    /**
     * Connect to host and port.
     *
     * @return Connect future. Fires when connected.
     */
    F connect();

    /**
     * Disconnect from the server
     *
     * @return Disconnect future. Fires when disconnected.
     */
    F disconnect();

    /**
     * Send data to server
     *
     * @param frame Data for sending
     * @return Write future. Will fire when the data is sent.
     */
    F send(Frame frame);
}
