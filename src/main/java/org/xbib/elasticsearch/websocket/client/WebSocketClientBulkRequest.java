package org.xbib.elasticsearch.websocket.client;

import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

public interface WebSocketClientBulkRequest extends WebSocketClientRequest {

    @Override
    WebSocketClientBulkRequest data(XContentBuilder builder);

    @Override
    void send(WebSocketClient client) throws IOException;
}
