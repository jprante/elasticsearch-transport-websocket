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

import java.net.InetSocketAddress;
import java.net.URI;
import org.elasticsearch.websocket.client.WebSocketActionListener;
import org.elasticsearch.websocket.client.WebSocketClient;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpHeaders.Values;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.websocketx.WebSocket00FrameDecoder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.util.CharsetUtil;

/**
 * Handles socket communication for a connected WebSocket client. Not intended
 * for end-users. Please use {@link NettyWebSocketClient} or
 * {@link NettyWebSocketActionListener} for controlling your client.
 *
 * @author <a href="http://www.pedantique.org/">Carl Bystr&ouml;m</a>
 */
public class NettyWebSocketClientHandler extends SimpleChannelUpstreamHandler
        implements WebSocketClient {

    private final ClientBootstrap bootstrap;
    private final URI url;
    private final WebSocketActionListener listener;
    private boolean handshakeCompleted = false;
    private Channel channel;

    public NettyWebSocketClientHandler(ClientBootstrap bootstrap, URI url, WebSocketActionListener listener) {
        this.bootstrap = bootstrap;
        this.url = url;
        this.listener = listener;
    }
    
    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
        String path = url.getPath();
        if (url.getQuery() != null && url.getQuery().length() > 0) {
            path = url.getPath() + "?" + url.getQuery();
        }
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
        request.addHeader(Names.UPGRADE, Values.WEBSOCKET);
        request.addHeader(Names.CONNECTION, Values.UPGRADE);
        request.addHeader(Names.HOST, url.getHost());
        request.addHeader(Names.ORIGIN, "http://" + url.getHost());
        event.getChannel().write(request);
        ctx.getPipeline().replace("encoder", "ws-encoder", new WebSocket00FrameEncoder());
        this.channel = event.getChannel();
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
        listener.onDisconnect(this);
        handshakeCompleted = false;
        channel = null;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
        if (!handshakeCompleted) {
            HttpResponse response = (HttpResponse) event.getMessage();
            final HttpResponseStatus status = new HttpResponseStatus(101, "Web Socket Protocol Handshake");
            final boolean validStatus = response.getStatus().equals(status);
            final boolean validUpgrade = response.getHeader(Names.UPGRADE).equals(Values.WEBSOCKET);
            final boolean validConnection = response.getHeader(Names.CONNECTION).equals(Values.UPGRADE);
            if (!validStatus || !validUpgrade || !validConnection) {
                throw new NettyWebSocketException("Invalid handshake response");
            }
            handshakeCompleted = true;
            ctx.getPipeline().replace("decoder", "ws-decoder", new WebSocket00FrameDecoder());
            listener.onConnect(this);
            return;
        }
        if (event.getMessage() instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) event.getMessage();
            throw new NettyWebSocketException("Unexpected HttpResponse (status=" + response.getStatus() + ", content=" + response.getContent().toString(CharsetUtil.UTF_8) + ")");
        }
        WebSocketFrame frame = (WebSocketFrame) event.getMessage();
        listener.onMessage(this, frame);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        final Throwable t = e.getCause();
        listener.onError(t);
        e.getChannel().close();
    }

    @Override
    public ChannelFuture connect() {
        return bootstrap.connect(new InetSocketAddress(url.getHost(), url.getPort()));
    }

    @Override
    public ChannelFuture disconnect() {
        if (channel == null) {
            return null;
        }
        return channel.close();
    }

    @Override
    public ChannelFuture send(WebSocketFrame frame) {
        if (channel == null) {
            return null;
        }
        return channel.write(frame);
    }

}
