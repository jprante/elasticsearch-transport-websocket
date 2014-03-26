
package org.xbib.elasticsearch.websocket.http;

public interface HttpServerAdapter {

    void dispatchRequest(HttpRequest request, HttpChannel channel);
}
