/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.websocket.http.netty;

import java.io.IOException;
import java.util.Map;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.websocket.InteractiveRequest;
import org.elasticsearch.websocket.client.WebSocketClient;
import org.elasticsearch.websocket.client.WebSocketClientRequest;

/**
 * Netty implemenation for an interactive request.
 * At the same time, this class serves as an implementation
 * for a WebSocket client request.
 * 
 * @author Jörg Prante <joergprante@gmail.com>
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
