
package org.xbib.elasticsearch.websocket;

import java.util.List;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.AbstractModule;

import org.xbib.elasticsearch.websocket.action.bulk.BulkDeleteAction;
import org.xbib.elasticsearch.websocket.action.bulk.BulkFlushAction;
import org.xbib.elasticsearch.websocket.action.bulk.BulkIndexAction;
import org.xbib.elasticsearch.websocket.action.pubsub.ForwardAction;
import org.xbib.elasticsearch.websocket.action.pubsub.PublishAction;
import org.xbib.elasticsearch.websocket.action.pubsub.SubscribeAction;
import org.xbib.elasticsearch.websocket.action.pubsub.UnsubscribeAction;

/**
 * The InteractiveActionModule binds all WebSocket actions.
 * 
 */
public class InteractiveActionModule extends AbstractModule {

    private List<Class<? extends BaseInteractiveHandler>> websocketActions = Lists.newArrayList();

    public InteractiveActionModule(List<Class<? extends BaseInteractiveHandler>> websocketActions) {
        this.websocketActions = websocketActions;
    }

    @Override
    protected void configure() {
        for (Class<? extends BaseInteractiveHandler> websocketAction : websocketActions) {
            bind(websocketAction).asEagerSingleton();
        }
        bind(PublishAction.class).asEagerSingleton();
        bind(SubscribeAction.class).asEagerSingleton();
        bind(UnsubscribeAction.class).asEagerSingleton();
        bind(ForwardAction.class).asEagerSingleton();
        bind(BulkDeleteAction.class).asEagerSingleton();
        bind(BulkIndexAction.class).asEagerSingleton();
        bind(BulkFlushAction.class).asEagerSingleton();
    }
}
