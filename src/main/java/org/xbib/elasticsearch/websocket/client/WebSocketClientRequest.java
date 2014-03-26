
package org.xbib.elasticsearch.websocket.client;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;

public interface WebSocketClientRequest {
        
    WebSocketClientRequest type(String type);
    
    WebSocketClientRequest data(XContentBuilder builder);    
    
    void send(WebSocketClient client) throws IOException;
}
