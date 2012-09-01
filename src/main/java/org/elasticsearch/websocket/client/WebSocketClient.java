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

package org.elasticsearch.websocket.client;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * A WebSocket client.
 *
 * @author <a href="http://www.pedantique.org/">Carl Bystr&ouml;m</a>
 * @author Jörg Prante <joergprante@gmail.com>
 */
public interface WebSocketClient<F extends ChannelFuture, Frame extends WebSocketFrame> {

    /**
     * The channel this client has opened.
     * @return the channel
     */
    Channel channel();
    
    /**
     * Connect to host and port.
     *
     * @return Connect future. Fires when connected.
     */
    F connect();

    /**
     * Disconnect from the server
     *
     * @return Disconnect future. Fires when disconnected.
     */
    F disconnect();

    /**
     * Send data to server
     *
     * @param frame Data for sending
     * @return Write future. Will fire when the data is sent.
     */
    F send(Frame frame);
}
