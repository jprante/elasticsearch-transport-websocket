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

import java.net.URI;
import java.util.concurrent.Executors;
import org.elasticsearch.websocket.client.WebSocketActionListener;
import org.elasticsearch.websocket.client.WebSocketClient;
import org.elasticsearch.websocket.client.WebSocketClientBulkRequest;
import org.elasticsearch.websocket.client.WebSocketClientFactory;
import org.elasticsearch.websocket.client.WebSocketClientRequest;
import org.elasticsearch.websocket.http.netty.NettyInteractiveRequest;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;

/**
 * A factory for creating WebSocket clients. The entry point for creating and
 * connecting a client. Can and should be used to create multiple instances.
 *
 * Extended for WebSocket client request methods.
 * 
 * @author <a href="http://www.pedantique.org/">Carl Bystr&ouml;m</a>
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class NettyWebSocketClientFactory implements WebSocketClientFactory {

    private NioClientSocketChannelFactory socketChannelFactory = new NioClientSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool());

    /**
     * Create a new WebSocket client
     *
     * @param url URL to connect to.
     * @param listener Callback interface to receive events
     * @return A WebSocket client. Call {@link NettyWebSocketClient#connect()} to
     * connect.
     */
    @Override
    public WebSocketClient newClient(final URI url, final WebSocketActionListener listener) {
        ClientBootstrap bootstrap = new ClientBootstrap(socketChannelFactory);

        String protocol = url.getScheme();
        if (!protocol.equals("ws") && !protocol.equals("wss")) {
            throw new IllegalArgumentException("unsupported protocol: " + protocol);
        }

        final NettyWebSocketClientHandler clientHandler = new NettyWebSocketClientHandler(bootstrap, url, listener);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("decoder", new HttpResponseDecoder());
                pipeline.addLast("encoder", new HttpRequestEncoder());
                pipeline.addLast("ws-handler", clientHandler);
                return pipeline;
            }
        });
        
        return clientHandler;
    }
    
    @Override
    public void shutdown() {        
        socketChannelFactory.releaseExternalResources();
    }
    
    @Override
    public WebSocketClientRequest newRequest() {
        return new NettyInteractiveRequest();
    }
    
    @Override
    public WebSocketClientBulkRequest indexRequest() {
        return new NettyWebSocketBulkRequest("index");
    }
    
    @Override
    public WebSocketClientBulkRequest deleteRequest() {
        return new NettyWebSocketBulkRequest("delete");
    }
    
    @Override
    public WebSocketClientRequest flushRequest() {
        return new NettyInteractiveRequest().type("flush");
    }
}
