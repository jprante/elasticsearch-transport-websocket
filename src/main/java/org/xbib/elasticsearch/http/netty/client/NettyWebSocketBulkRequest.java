package org.xbib.elasticsearch.http.netty.client;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.xbib.elasticsearch.websocket.client.WebSocketClient;
import org.xbib.elasticsearch.websocket.client.WebSocketClientBulkRequest;
import org.xbib.elasticsearch.http.netty.NettyInteractiveRequest;
import org.xbib.elasticsearch.http.netty.NettyInteractiveResponse;

import java.io.IOException;

/**
 * Netty bulk request convenience class.
 */
public class NettyWebSocketBulkRequest
        extends NettyInteractiveRequest
        implements WebSocketClientBulkRequest {

    public NettyWebSocketBulkRequest(String type) {
        super.type(type);
    }

    @Override
    public NettyWebSocketBulkRequest data(XContentBuilder builder) {
        super.data(builder);
        return this;
    }

    @Override
    public void send(WebSocketClient client) throws IOException {
        client.send(new NettyInteractiveResponse(super.type, super.builder).response());
    }

}
