
package org.xbib.elasticsearch.websocket.http;

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
     * @return the bound transport addess
     */
    BoundTransportAddress boundAddress();

    /**
     * Get HTTP statistics
     * @return a statistics object
     */
    HttpStats stats();

    /**
     * Set HTTP server adapter
     * @param httpServerAdapter 
     */
    void httpServerAdapter(HttpServerAdapter httpServerAdapter);

    /**
     * Set WebSocket server adapter
     * @param webSocketServerAdapter 
     */
    void webSocketServerAdapter(WebSocketServerAdapter webSocketServerAdapter);

    /**
     * Find channel for a given channel ID
     * @param id
     * @return channel or null
     */
    Channel channel(Integer id);

    /**
     * Forward a message to a node for a given channel ID
     * @param nodeAdress
     * @param channelId
     * @param message 
     */
    void forward(String nodeAdress, Integer channelId, XContentBuilder message);
}
