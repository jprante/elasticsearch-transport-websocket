package org.xbib.elasticsearch.http;

public interface HttpServerAdapter {

    void dispatchRequest(HttpRequest request, HttpChannel channel);
}
