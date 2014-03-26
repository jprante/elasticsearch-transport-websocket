
package org.xbib.elasticsearch.websocket.http.netty;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.rest.support.RestUtils;
import org.jboss.netty.handler.codec.http.HttpMethod;

import java.util.Map;

import org.xbib.elasticsearch.websocket.common.bytes.ChannelBufferBytesReference;
import org.xbib.elasticsearch.websocket.http.HttpRequest;

import static org.elasticsearch.common.collect.Maps.newHashMap;

public class NettyHttpRequest extends HttpRequest {

    private final org.jboss.netty.handler.codec.http.HttpRequest request;

    private final Map<String, String> params;

    private final String rawPath;

    private final BytesReference content;

    private final String uri;

    public NettyHttpRequest(org.jboss.netty.handler.codec.http.HttpRequest request) {
        this.request = request;
        this.params = newHashMap();
        this.content = request.getContent().readable() ?
                new ChannelBufferBytesReference(request.getContent()) :
                BytesArray.EMPTY;
        this.uri = request.getUri();
        int pathEndPos = uri.indexOf('?');
        if (pathEndPos < 0) {
            this.rawPath = uri;
        } else {
            this.rawPath = uri.substring(0, pathEndPos);
            RestUtils.decodeQueryString(uri, pathEndPos + 1, params);
        }
    }

    @Override
    public Method method() {
        HttpMethod httpMethod = request.getMethod();
        if (httpMethod == HttpMethod.GET) {
            return Method.GET;
        }

        if (httpMethod == HttpMethod.POST) {
            return Method.POST;
        }

        if (httpMethod == HttpMethod.PUT) {
            return Method.PUT;
        }

        if (httpMethod == HttpMethod.DELETE) {
            return Method.DELETE;
        }

        if (httpMethod == HttpMethod.HEAD) {
            return Method.HEAD;
        }

        if (httpMethod == HttpMethod.OPTIONS) {
            return Method.OPTIONS;
        }

        return Method.GET;
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public String rawPath() {
        return rawPath;
    }

    @Override
    public Map<String, String> params() {
        return params;
    }

    @Override
    public boolean hasContent() {
        return content.length() > 0;
    }

    @Override
    public boolean contentUnsafe() {
        // Netty http decoder always copies over the http content
        return false;
    }

    @Override
    public BytesReference content() {
        return content;
    }

    @Override
    public String header(String name) {
        return request.headers().get(name);
    }

    @Override
    public Iterable<Map.Entry<String, String>> headers() {
        return request.headers().entries();
    }

    @Override
    public boolean hasParam(String key) {
        return params.containsKey(key);
    }

    @Override
    public String param(String key) {
        return params.get(key);
    }

    @Override
    public String param(String key, String defaultValue) {
        String value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
