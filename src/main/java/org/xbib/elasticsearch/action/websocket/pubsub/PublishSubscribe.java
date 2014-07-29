package org.xbib.elasticsearch.action.websocket.pubsub;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.xbib.elasticsearch.websocket.BaseInteractiveHandler;
import org.xbib.elasticsearch.websocket.InteractiveController;
import org.xbib.elasticsearch.http.HttpServerTransport;

/**
 * Base class for Publish/Subscribe actions
 */
public abstract class PublishSubscribe extends BaseInteractiveHandler {

    protected final String pubSubIndexName;

    protected final HttpServerTransport transport;

    protected final Checkpointer service;

    protected final TimeValue scrollTimeout = new TimeValue(60000);

    protected final int scrollSize = 100;

    public PublishSubscribe(Settings settings,
                            Client client,
                            HttpServerTransport transport,
                            InteractiveController controller,
                            Checkpointer service) {
        super(settings, client);
        this.pubSubIndexName = PubSubIndexName.Conf.indexName(settings);
        this.transport = transport;
        this.service = service;
    }

}
