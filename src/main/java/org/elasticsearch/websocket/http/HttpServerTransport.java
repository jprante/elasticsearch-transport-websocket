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

package org.elasticsearch.websocket.http;

import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.transport.BoundTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.jboss.netty.channel.Channel;

/**
 * HttpServerTransport extended by WebSocket services
 */
public interface HttpServerTransport extends LifecycleComponent<HttpServerTransport> {

    /**
     * Return the bound transport addess
     * @return the bound transport addess
     */
    BoundTransportAddress boundAddress();

    /**
     * Get HTTP statistics
     * @return a statistics object
     */
    HttpStats stats();

    /**
     * Set HTTP server adapter
     * @param httpServerAdapter 
     */
    void httpServerAdapter(HttpServerAdapter httpServerAdapter);

    /**
     * Set WebSocket server adapter
     * @param webSocketServerAdapter 
     */
    void webSocketServerAdapter(WebSocketServerAdapter webSocketServerAdapter);

    /**
     * Find channel for a given channel ID
     * @param id
     * @return channel or null
     */
    Channel channel(Integer id);

    /**
     * Forward a message to a node for a given channel ID
     * @param nodeAdress
     * @param channelId
     * @param message 
     */
    void forward(String nodeAdress, Integer channelId, XContentBuilder message);
}
