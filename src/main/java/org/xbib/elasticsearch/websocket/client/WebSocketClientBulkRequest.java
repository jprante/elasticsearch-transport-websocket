
package org.xbib.elasticsearch.websocket.client;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;

public interface WebSocketClientBulkRequest extends WebSocketClientRequest {

    @Override
    WebSocketClientBulkRequest data(XContentBuilder builder);

    @Override
    void send(WebSocketClient client) throws IOException;
}
