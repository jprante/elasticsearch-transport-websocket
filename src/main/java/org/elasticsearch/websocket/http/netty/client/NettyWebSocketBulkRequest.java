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

package org.elasticsearch.websocket.http.netty.client;

import java.io.IOException;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.websocket.client.WebSocketClient;
import org.elasticsearch.websocket.client.WebSocketClientBulkRequest;
import org.elasticsearch.websocket.http.netty.NettyInteractiveRequest;
import org.elasticsearch.websocket.http.netty.NettyInteractiveResponse;

/**
 * Netty bulk request convenience class.
 * 
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class NettyWebSocketBulkRequest 
    extends NettyInteractiveRequest 
    implements WebSocketClientBulkRequest {
    
    public NettyWebSocketBulkRequest(String type) {
        super.type(type);
    }
    
    @Override
    public NettyWebSocketBulkRequest data(XContentBuilder builder) {
        super.data(builder);
        return this;
    }

    @Override
    public void send(WebSocketClient client) throws IOException {
        client.send(new NettyInteractiveResponse(super.type, super.builder).response());
    }

}
