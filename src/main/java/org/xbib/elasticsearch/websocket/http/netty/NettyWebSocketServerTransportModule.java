
package org.xbib.elasticsearch.websocket.http.netty;

import org.elasticsearch.common.inject.AbstractModule;
import org.xbib.elasticsearch.websocket.http.HttpServerTransport;

/**
 * Module for HttpServerModule.
 * 
 */
public class NettyWebSocketServerTransportModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(HttpServerTransport.class).to(NettyWebSocketServerTransport.class).asEagerSingleton();
    }
}
