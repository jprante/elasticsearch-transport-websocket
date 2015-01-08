package org.xbib.elasticsearch.rest.action.websocket;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.xbib.elasticsearch.rest.XContentRestResponse;
import org.xbib.elasticsearch.rest.XContentThrowableRestResponse;
import org.xbib.elasticsearch.action.websocket.pubsub.PubSubIndexName;

import java.io.IOException;

import static org.elasticsearch.rest.RestStatus.BAD_REQUEST;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.xbib.elasticsearch.rest.RestXContentBuilder.restContentBuilder;

/**
 * Unsubscribe action for REST
 */
public class RestUnsubscribeAction extends BaseRestHandler {

    private final String pubSubIndexName;

    @Inject
    public RestUnsubscribeAction(Settings settings, Client client,
                                 RestController restController) {
        super(settings, restController, client);
        this.pubSubIndexName = PubSubIndexName.Conf.indexName(settings);
        restController.registerHandler(RestRequest.Method.GET, "/_unsubscribe", this);
        restController.registerHandler(RestRequest.Method.POST, "/_unsubscribe", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, Client client) {
        String subscriberId = request.hasParam("subscriber") ? request.param("subscriber") : null;
        if (subscriberId == null) {
            try {
                channel.sendResponse(new XContentThrowableRestResponse(request, new IllegalArgumentException("no subscriber")));
            } catch (IOException e) {
                logger.error("error while sending failure response", e);
            }
            return;
        }
        try {
            client.prepareDelete(pubSubIndexName, "subscribe", subscriberId)
                    .execute(new ActionListener<DeleteResponse>() {
                        @Override
                        public void onResponse(DeleteResponse response) {
                            try {
                                XContentBuilder builder = restContentBuilder(request);
                                builder.startObject().field("ok", true).field("id", response.getId()).endObject();
                                channel.sendResponse(new XContentRestResponse(request, OK, builder));
                            } catch (Exception e) {
                                onFailure(e);
                            }
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            try {
                                logger.error("error while processing unsubscribe request", e);
                                channel.sendResponse(new XContentThrowableRestResponse(request, e));
                            } catch (IOException e1) {
                                logger.error("error while sending error response", e1);
                            }
                        }
                    });
        } catch (Exception e) {
            try {
                XContentBuilder builder = restContentBuilder(request);
                channel.sendResponse(new XContentRestResponse(request, BAD_REQUEST, builder.startObject().field("error", e.getMessage()).endObject()));
            } catch (IOException e1) {
                logger.error("exception while sending exception response", e1);
            }
        }
    }

}
