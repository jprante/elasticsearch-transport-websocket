
package org.xbib.elasticsearch.websocket.http.netty;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;

import org.xbib.elasticsearch.websocket.InteractiveRequest;
import org.xbib.elasticsearch.websocket.client.WebSocketClient;
import org.xbib.elasticsearch.websocket.client.WebSocketClientRequest;

/**
 * Netty implemenation for an interactive request.
 * At the same time, this class serves as an implementation
 * for a WebSocket client request.
 */
public class NettyInteractiveRequest implements InteractiveRequest, WebSocketClientRequest {

    protected String type; 
    
    protected Map<String, Object> data;    

    protected XContentBuilder builder;
    
    public NettyInteractiveRequest() {
    }
    
    public NettyInteractiveRequest(Map<String, Object> data) {
        this.data = data;
    }
    
    @Override
    public NettyInteractiveRequest type(String type) {
        this.type = type;
        return this;
    }
    
    @Override
    public NettyInteractiveRequest data(XContentBuilder builder) {
        this.builder = builder;
        return this;
    }
    
    @Override
    public void send(WebSocketClient client) throws IOException {
        client.send(new NettyInteractiveResponse(type, builder).response());
    }

    @Override
    public Map<String, Object> asMap() {
        return data;
    }

    @Override
    public boolean hasParam(String key) {
        return data.containsKey(key);
    }

    @Override
    public Object param(String key) {
        return data.get(key);
    }

    @Override
    public String paramAsString(String key) {
        Object o = param(key);
        return o != null ? o.toString() : null;
    }

    @Override
    public String paramAsString(String key, String defaultValue) {
        Object o = param(key);
        return o != null ? o.toString() : defaultValue;
    }

    @Override
    public long paramAsLong(String key) {
        Object o = param(key);
        try {
            return o != null ? Long.parseLong(o.toString()) : null;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public long paramAsLong(String key, long defaultValue) {
        Object o = param(key);
        try {
            return o != null ? Long.parseLong(o.toString()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean paramAsBoolean(String key) {
        Object o = param(key);
        return o != null ? Boolean.getBoolean(o.toString()) : null;
    }

    @Override
    public boolean paramAsBoolean(String key, boolean defaultValue) {
        Object o = param(key);
        return o != null ? Boolean.getBoolean(o.toString()) : defaultValue;
    }

    @Override
    public TimeValue paramAsTime(String key) {
        Object o = param(key);
        return o != null ? TimeValue.parseTimeValue(o.toString(), null) : null;
    }

    @Override
    public TimeValue paramAsTime(String key, TimeValue defaultValue) {
        Object o = param(key);
        return o != null ? TimeValue.parseTimeValue(o.toString(), defaultValue) : defaultValue;
    }
}
