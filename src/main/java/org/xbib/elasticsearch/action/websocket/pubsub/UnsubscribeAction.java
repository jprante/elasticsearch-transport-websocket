package org.xbib.elasticsearch.action.websocket.pubsub;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.xbib.elasticsearch.websocket.BaseInteractiveHandler;
import org.xbib.elasticsearch.websocket.InteractiveChannel;
import org.xbib.elasticsearch.websocket.InteractiveController;
import org.xbib.elasticsearch.websocket.InteractiveRequest;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Unsubscribe action. It removes a subscription.
 */
public class UnsubscribeAction extends BaseInteractiveHandler {

    private final String TYPE = "unsubscribe";
    private final String pubSubIndexName;

    @Inject
    public UnsubscribeAction(Settings settings,
                             Client client,
                             InteractiveController controller) {
        super(settings, client);
        this.pubSubIndexName = PubSubIndexName.Conf.indexName(settings);
        controller.registerHandler(TYPE, this);
    }

    @Override
    public void handleRequest(final InteractiveRequest request, final InteractiveChannel channel) {
        String subscriberId = request.hasParam("subscriber") ? request.paramAsString("subscriber") : null;
        if (subscriberId == null) {
            try {
                channel.sendResponse(TYPE, new IllegalArgumentException("no subscriber"));
            } catch (IOException e) {
                logger.error("error while sending failure response", e);
            }
        }
        try {
            client.prepareDelete(pubSubIndexName, SubscribeAction.TYPE, subscriberId)
                    .execute(new ActionListener<DeleteResponse>() {
                        @Override
                        public void onResponse(DeleteResponse response) {
                            try {
                                XContentBuilder builder = jsonBuilder();
                                builder.startObject().field("ok", true).field("id", response.getId()).endObject();
                                channel.sendResponse(TYPE, builder);
                            } catch (Exception e) {
                                onFailure(e);
                            }
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            logger.error("error while processing unsubscribe request", e);
                            try {
                                channel.sendResponse(TYPE, e);
                            } catch (IOException ex) {
                                logger.error("error while sending error response", ex);
                            }
                        }
                    });
        } catch (Exception e) {
            logger.error("exception while processing unsubscribe request", e);
            try {
                channel.sendResponse(TYPE, e);
            } catch (IOException e1) {
                logger.error("exception while sending exception response", e1);
            }
        }
    }
}
