package org.xbib.elasticsearch.rest.action.websocket;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.jboss.netty.channel.Channel;
import org.xbib.elasticsearch.action.websocket.pubsub.Checkpointer;
import org.xbib.elasticsearch.action.websocket.pubsub.PubSubIndexName;
import org.xbib.elasticsearch.http.HttpServerTransport;
import org.xbib.elasticsearch.http.netty.NettyInteractiveResponse;
import org.xbib.elasticsearch.rest.XContentRestResponse;
import org.xbib.elasticsearch.rest.XContentThrowableRestResponse;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.rest.RestStatus.BAD_REQUEST;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.xbib.elasticsearch.rest.RestXContentBuilder.restContentBuilder;

/**
 * Publish action for REST
 */
public class RestPublishAction extends BaseRestHandler {

    private final static String TYPE = "publish";

    private final String pubSubIndexName;

    private final HttpServerTransport transport;

    private final Checkpointer service;

    @Inject
    public RestPublishAction(Settings settings, Client client,
                             RestController restController,
                             HttpServerTransport transport,
                             Checkpointer service) {
        super(settings, client);
        this.pubSubIndexName = PubSubIndexName.Conf.indexName(settings);
        this.transport = transport;
        this.service = service;
        restController.registerHandler(RestRequest.Method.GET, "/_publish", this);
        restController.registerHandler(RestRequest.Method.POST, "/_publish", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, Client client) {
        String topic = request.hasParam("topic") ? request.param("topic") : "*";
        try {
            final XContentBuilder messageBuilder = createPublishMessage(request);
            client.prepareIndex()
                    .setIndex(pubSubIndexName)
                    .setType(TYPE)
                    .setSource(messageBuilder)
                    .setRefresh(request.paramAsBoolean("refresh", true))
                    .execute(new ActionListener<IndexResponse>() {
                        @Override
                        public void onResponse(IndexResponse response) {
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
                                logger.error("Error processing publish request", e);
                                channel.sendResponse(new XContentThrowableRestResponse(request, e));
                            } catch (IOException e1) {
                                logger.error("Failed to send failure response", e1);
                            }
                        }
                    });
            // push phase - scroll over subscribers for this topic currently connected
            QueryBuilder queryBuilder = termQuery("topic", topic);
            SearchResponse searchResponse = client.prepareSearch()
                    .setIndices(pubSubIndexName)
                    .setTypes("subscribe")
                    .setSearchType(SearchType.SCAN)
                    .setScroll(new TimeValue(60000))
                    .setQuery(queryBuilder)
                    .addField("subscriber.channel")
                    .setSize(100)
                    .execute().actionGet();
            messageBuilder.close();
            service.checkpoint(topic);
            // push phase - write the message to the subscribers. We have 60 seconds per 100 subscribers.
            while (true) {
                searchResponse = client.prepareSearchScroll(searchResponse.getScrollId())
                        .setScroll(new TimeValue(60000))
                        .execute().actionGet();
                for (SearchHit hit : searchResponse.getHits()) {
                    service.checkpoint(hit.getId());
                    SearchHitField channelField = hit.field("subscriber.channel");
                    Map<String, Object> channelfieldMap = channelField.getValue();
                    Integer id = (Integer) channelfieldMap.get("id");
                    Channel ch = transport.channel(id);
                    if (ch != null) {
                        ch.write(new NettyInteractiveResponse("message", messageBuilder).response());
                    }
                }
                if (searchResponse.getHits().hits().length == 0) {
                    break;
                }
            }
            service.flushCheckpoint();
        } catch (Exception e) {
            try {
                XContentBuilder builder = restContentBuilder(request);
                channel.sendResponse(new XContentRestResponse(request, BAD_REQUEST, builder.startObject().field("error", e.getMessage()).endObject()));
            } catch (IOException e1) {
                logger.error("Failed to send failure response", e1);
            }
        }
    }

    private XContentBuilder createPublishMessage(RestRequest request) {
        try {
            Map<String, Object> map = null;
            String message = request.content().toUtf8();
            XContentParser parser = null;
            try {
                parser = XContentFactory.xContent(message).createParser(message);
                map = parser.map();
            } catch (Exception e) {
                logger.warn("unable to parse {}", message);
            } finally {
                parser = null;
            }
            return jsonBuilder().startObject()
                    .field("timestamp", request.param("timestamp", Long.toString(System.currentTimeMillis())))
                    .field("message", map)
                    .endObject();
        } catch (IOException e) {
            return null;
        }
    }
}
