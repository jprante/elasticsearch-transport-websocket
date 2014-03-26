
package org.xbib.elasticsearch.websocket.http.netty.client;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;

import org.xbib.elasticsearch.websocket.client.WebSocketClient;

/**
 * A Websocket client.
 */
public interface NettyWebSocketClient extends WebSocketClient<ChannelFuture,WebSocketFrame> {

}
