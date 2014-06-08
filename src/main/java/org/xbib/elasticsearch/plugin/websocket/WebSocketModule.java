package org.xbib.elasticsearch.plugin.websocket;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;
import org.xbib.elasticsearch.rest.HttpPatchRestController;
import org.xbib.elasticsearch.websocket.BaseInteractiveHandler;
import org.xbib.elasticsearch.websocket.InteractiveActionModule;
import org.xbib.elasticsearch.websocket.InteractiveController;

import java.util.List;

public class WebSocketModule extends AbstractModule {

    private final Settings settings;

    private List<Class<? extends BaseInteractiveHandler>> webSocketActions = Lists.newArrayList();

    public WebSocketModule(Settings settings) {
        this.settings = settings;
    }

    public void addInteractiveAction(Class<? extends BaseInteractiveHandler> action) {
        webSocketActions.add(action);
    }

    @Override
    protected void configure() {
        bind(InteractiveController.class).asEagerSingleton();
        new InteractiveActionModule(webSocketActions).configure(binder());
        bind(HttpPatchRestController.class).asEagerSingleton();
    }

}
