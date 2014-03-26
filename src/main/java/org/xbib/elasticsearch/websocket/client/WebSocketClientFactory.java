
package org.xbib.elasticsearch.websocket.client;

import java.net.URI;

/**
 * A WebSocketClientFactory has methods for creating WebSocket clients
 * and for creating WebSocket requests.
 */
public interface WebSocketClientFactory {
    
    WebSocketClient newClient(URI resourceIdentifier, WebSocketActionListener listener);
    
    WebSocketClientRequest newRequest();
    
    WebSocketClientBulkRequest indexRequest();
        
    WebSocketClientBulkRequest deleteRequest();

    WebSocketClientRequest flushRequest();
    
    void shutdown();
}
