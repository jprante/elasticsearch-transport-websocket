
package org.xbib.elasticsearch.websocket.http;

import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.Modules;
import org.elasticsearch.common.inject.SpawnModules;
import org.elasticsearch.common.settings.Settings;

import org.xbib.elasticsearch.websocket.http.netty.NettyWebSocketServerTransportModule;

public class HttpServerModule extends AbstractModule implements SpawnModules {

    private final Settings settings;

    public HttpServerModule(Settings settings) {
        this.settings = settings;
    }

    @Override
    public Iterable<? extends Module> spawnModules() {
        return ImmutableList.of(Modules.createModule(settings.getAsClass("websocket.type", NettyWebSocketServerTransportModule.class, "org.elasticsearch.websocket.http.", "HttpServerTransportModule"), settings));
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected void configure() {
        bind(HttpServer.class).asEagerSingleton();
    }
}
