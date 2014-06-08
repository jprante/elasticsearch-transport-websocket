package org.xbib.elasticsearch.websocket.client;

import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

public interface WebSocketClientRequest {

    WebSocketClientRequest type(String type);

    WebSocketClientRequest data(XContentBuilder builder);

    void send(WebSocketClient client) throws IOException;
}
