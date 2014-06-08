package org.xbib.elasticsearch.websocket;

/**
 * A request handler for interactive requests
 */
public interface InteractiveHandler {

    void handleRequest(InteractiveRequest request, InteractiveChannel channel);

}
