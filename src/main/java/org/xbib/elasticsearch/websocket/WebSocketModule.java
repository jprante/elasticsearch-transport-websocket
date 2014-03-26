
package org.xbib.elasticsearch.websocket;

import java.util.List;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;

/**
 * The WebSocketModule sets up the InteractiveController
 * and the InteractiveActionModule.
 * 
 */
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
    }

}
