package org.xbib.elasticsearch.http;

import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.transport.BoundTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.jboss.netty.channel.Channel;

/**
 * HttpServerTransport extended by Websocket services
 */
public interface HttpServerTransport extends LifecycleComponent<HttpServerTransport> {

    /**
     * Return the bound transport addess
     *
     * @return the bound transport addess
     */
    BoundTransportAddress boundAddress();

    HttpInfo info();

    /**
     * Get HTTP statistics
     *
     * @return a statistics object
     */
    HttpStats stats();

    /**
     * Set HTTP server adapter
     *
     * @param httpServerAdapter HTTP server adapter
     */
    void httpServerAdapter(HttpServerAdapter httpServerAdapter);

    /**
     * Set WebSocket server adapter
     *
     * @param webSocketServerAdapter web socket server adapter
     */
    void webSocketServerAdapter(WebSocketServerAdapter webSocketServerAdapter);

    /**
     * Find channel for a given channel ID
     *
     * @param id id
     * @return channel or null
     */
    Channel channel(Integer id);

    /**
     * Forward a message to a node for a given channel ID
     *
     * @param nodeAdress node address
     * @param channelId channel ID
     * @param message message
     */
    void forward(String nodeAdress, Integer channelId, XContentBuilder message);
}
