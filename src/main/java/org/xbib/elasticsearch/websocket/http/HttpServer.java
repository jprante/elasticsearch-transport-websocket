
package org.xbib.elasticsearch.websocket.http;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.service.NodeService;
import org.elasticsearch.rest.RestController;

import org.xbib.elasticsearch.websocket.InteractiveController;
import org.xbib.elasticsearch.websocket.Presence;


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
        
        // the hook into nodeService can not be used here
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
    protected void doStart() throws ElasticsearchException {
        transport.start();
        logger.info("{}", transport.boundAddress());
        nodeService.putAttribute("websocket_address", transport.boundAddress().publishAddress().toString());
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        nodeService.removeAttribute("websocket_address");
        transport.stop();
    }

    @Override
    protected void doClose() throws ElasticsearchException {
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
