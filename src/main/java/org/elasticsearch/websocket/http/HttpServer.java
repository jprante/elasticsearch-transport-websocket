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

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.service.NodeService;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.websocket.InteractiveController;
import org.elasticsearch.websocket.Presence;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;

/**
 * HttpServer with a REST and WebSocket interactive controller.
 */
public class HttpServer extends AbstractLifecycleComponent<HttpServer> {

    private final Environment environment;

    private final HttpServerTransport transport;

    private final RestController restController;
    
    private final InteractiveController interActiveController;

    private final NodeService nodeService;    
    
    @Inject
    public HttpServer(Settings settings, Environment environment, 
                      HttpServerTransport transport,
                      RestController restController,
                      InteractiveController interActiveController,
                      NodeService nodeService) {
        super(settings);
        this.environment = environment;
        this.transport = transport;
        this.restController = restController;
        this.interActiveController = interActiveController;
        this.nodeService = nodeService;

        transport.httpServerAdapter(new Dispatcher(this));
        transport.webSocketServerAdapter(new Interactor(this));
        
        // the hook into nodeService is used for optional node info 
        // (not usable with relocated HttpServer class)
        
        //nodeService.setHttpServer(this);

    }

    static class Dispatcher implements HttpServerAdapter {

        private final HttpServer server;

        Dispatcher(HttpServer server) {
            this.server = server;
        }

        @Override
        public void dispatchRequest(HttpRequest request, HttpChannel channel) {
            server.internalDispatchRequest(request, channel);
        }
    }

    static class Interactor implements WebSocketServerAdapter {

        private final HttpServer server;

        Interactor(HttpServer server) {
            this.server = server;
        }
        
        @Override
        public void presence(Presence presence, String topic, Channel channel) {
            server.internalPresence(presence, topic, channel);
        }

        @Override
        public void frame(WebSocketServerHandshaker handshaker, WebSocketFrame frame, ChannelHandlerContext context) {
            server.internalFrame(handshaker, frame, context);
        }
    }

    @Override
    protected void doStart() throws ElasticSearchException {
        transport.start();
        logger.info("{}", transport.boundAddress());
        nodeService.putAttribute("websocket_address", transport.boundAddress().publishAddress().toString());
    }

    @Override
    protected void doStop() throws ElasticSearchException {
        nodeService.removeAttribute("websocket_address");
        transport.stop();
    }

    @Override
    protected void doClose() throws ElasticSearchException {
        transport.close();
    }

    public HttpInfo info() {
        return new HttpInfo(transport.boundAddress());
    }

    public HttpStats stats() {
        return transport.stats();
    }
    
    public Channel channel(Integer id) {
        return transport.channel(id);
    }

    public void internalDispatchRequest(final HttpRequest request, final HttpChannel channel) {
        restController.dispatchRequest(request, channel);
    }
    
    public void internalPresence(Presence presence, String topic, Channel channel) {
        interActiveController.presence(presence, topic, channel);
    }
    
    public void internalFrame(WebSocketServerHandshaker handshaker, WebSocketFrame frame, ChannelHandlerContext context) {
        interActiveController.frame(handshaker, frame, context);   
    }
    
}
