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
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.websocket.InteractiveResponse;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * Netty implementation of an interactive response
 * 
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class NettyInteractiveResponse implements InteractiveResponse {

    private final String type;
    private final TextWebSocketFrame response;

    public NettyInteractiveResponse(String type, XContentBuilder builder) throws IOException {
        this.type = type;
        XContentBuilder responseBuilder = jsonBuilder()
                .startObject().field("ok", true).field("type", type);
        if (builder != null) {
            responseBuilder.rawField("data", builder.bytes());
        }
        responseBuilder.endObject();
        this.response = new TextWebSocketFrame(responseBuilder.string());
    }

    public NettyInteractiveResponse(String type, Map<String, Object> map) throws IOException {
        this.type = type;
        XContentBuilder responseBuilder = jsonBuilder();
        responseBuilder.startObject().field("ok", true).field("type", type).field("data", map).endObject();
        this.response = new TextWebSocketFrame(responseBuilder.string());
    }

    public NettyInteractiveResponse(String type, Throwable t) {
        this.type = type;
        this.response = new TextWebSocketFrame("{\"ok\":false,\"type\":\"" + type + "\",\"error\":\"" + t.getMessage() + "\"");
    }

    @Override
    public String type() {
        return type;
    }

    /**
     * The response frame with content, ready for writing to a Channel
     *
     * @return a TextWebSocketFrame
     * @throws IOException
     */
    @Override
    public TextWebSocketFrame response() throws IOException {
        return response;
    }
}
