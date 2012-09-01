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

import org.elasticsearch.websocket.Presence;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

/**
 *  Handles HTTP request and upgrades HTTP to WebSocket if appropriate.
 * 
 * 
 */
@ChannelHandler.Sharable
public class NettyHttpRequestHandler extends SimpleChannelUpstreamHandler {

    protected final NettyWebSocketServerTransport serverTransport;
    protected WebSocketServerHandshaker handshaker;

    private static String getWebSocketLocation(HttpRequest req) {
        return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + WEBSOCKET_PATH;
    }
    private static final String WEBSOCKET_PATH = "/websocket";
    
    public NettyHttpRequestHandler(NettyWebSocketServerTransport serverTransport) {
        this.serverTransport = serverTransport;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        HttpRequest request = (HttpRequest) e.getMessage();
        if (request.getUri().startsWith(WEBSOCKET_PATH)) {
            // Websocket handshake
            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(request), null, false);
            handshaker = wsFactory.newHandshaker(request);
            if (handshaker == null) {
                wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
            } else {
                handshaker.handshake(ctx.getChannel(), request).addListener(WebSocketServerHandshaker.HANDSHAKE_LISTENER);                
            }
            // extract topic from request URI
            String topic = request.getUri();
            if (topic.length() > WEBSOCKET_PATH.length() + 1) {
                topic = topic.substring(WEBSOCKET_PATH.length() + 1);
                serverTransport.presence(Presence.CONNECTED, topic, e.getChannel());
            }
        } else {
            serverTransport.dispatchRequest(new NettyHttpRequest(request), new NettyHttpChannel(serverTransport, e.getChannel(), request));
            super.messageReceived(ctx, e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        serverTransport.exceptionCaught(ctx, e);
        e.getChannel().close();
    }

}
