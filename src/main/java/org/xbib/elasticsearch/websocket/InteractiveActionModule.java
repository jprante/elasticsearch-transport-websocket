package org.xbib.elasticsearch.websocket;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.AbstractModule;
import org.xbib.elasticsearch.action.websocket.bulk.BulkDeleteAction;
import org.xbib.elasticsearch.action.websocket.bulk.BulkFlushAction;
import org.xbib.elasticsearch.action.websocket.bulk.BulkIndexAction;
import org.xbib.elasticsearch.action.websocket.pubsub.ForwardAction;
import org.xbib.elasticsearch.action.websocket.pubsub.PublishAction;
import org.xbib.elasticsearch.action.websocket.pubsub.SubscribeAction;
import org.xbib.elasticsearch.action.websocket.pubsub.UnsubscribeAction;

import java.util.List;

/**
 * The InteractiveActionModule binds all WebSocket actions.
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
