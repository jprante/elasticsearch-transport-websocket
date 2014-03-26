
package org.xbib.elasticsearch.plugin.websocket;

import java.util.Collection;

import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;

import static org.elasticsearch.common.collect.Lists.newArrayList;

import org.xbib.elasticsearch.websocket.WebSocketModule;
import org.xbib.elasticsearch.websocket.action.pubsub.Checkpointer;
import org.xbib.elasticsearch.websocket.http.HttpServer;
import org.xbib.elasticsearch.websocket.http.HttpServerModule;
import org.xbib.elasticsearch.websocket.rest.action.RestPublishAction;
import org.xbib.elasticsearch.websocket.rest.action.RestUnsubscribeAction;

/**
 * Websocket plugin
 */
public class WebSocketPlugin extends AbstractPlugin {

    private final Settings settings;

    public WebSocketPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "transport-websocket-"
                + Build.getInstance().getVersion() + "-"
                + Build.getInstance().getShortHash();
    }

    @Override
    public String description() {
        return "Websocket transport plugin";
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = newArrayList();
        if (settings.getAsBoolean("websocket.enabled", true)) {
            modules.add(HttpServerModule.class);
            modules.add(WebSocketModule.class);
        }
        return modules;
    }

   @Override public Collection<Class<? extends LifecycleComponent>> services() {
        Collection<Class<? extends LifecycleComponent>> services = newArrayList();
        if (settings.getAsBoolean("websocket.enabled", true)) {
            services.add(HttpServer.class);
            services.add(Checkpointer.class);
        }
        return services;
    }    
  
    public void onModule(RestModule module) {
        module.addRestAction(RestPublishAction.class);
        module.addRestAction(RestUnsubscribeAction.class);
    }

}
