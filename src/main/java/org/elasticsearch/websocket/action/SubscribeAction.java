/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.websocket.action;

import java.io.IOException;
import java.util.Map;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.FilterBuilders.numericRangeFilter;
import org.elasticsearch.index.query.NumericRangeFilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.websocket.InteractiveChannel;
import org.elasticsearch.websocket.InteractiveController;
import org.elasticsearch.websocket.InteractiveRequest;
import org.elasticsearch.websocket.http.HttpServerTransport;
import org.elasticsearch.websocket.http.netty.NettyInteractiveResponse;
import org.jboss.netty.channel.Channel;

/**
 * Subscribe action. It performs the subscription of a client to
 * the pubsub index under a given topic.
 * 
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class SubscribeAction extends PublishSubscribe {

    protected final static String TYPE = "subscribe";

    @Inject
    public SubscribeAction(Settings settings,
            Client client,
            HttpServerTransport transport,
            InteractiveController controller,
            Checkpointer service) {
        super(settings, client, transport, controller, service);
        controller.registerHandler(TYPE, this);
    }

    @Override
    public void handleRequest(final InteractiveRequest request, final InteractiveChannel channel) {
        final String topic = request.hasParam("topic") ? request.paramAsString("topic") : "*";
        final String subscriberId = request.hasParam("subscriber") ? request.paramAsString("subscriber") : null;
        if (subscriberId == null) {
            try {
                channel.sendResponse(TYPE, new IllegalArgumentException("no subscriber"));
            } catch (IOException e) {
                logger.error("error while sending failure response", e);
            }
        }
        try {
            client.prepareIndex()
                    .setIndex(pubSubIndexName)
                    .setType(TYPE)
                    .setId(subscriberId)
                    .setSource(createSubscriberMessage(topic, channel))
                    .setRefresh(request.paramAsBoolean("refresh", true))
                    .execute(new ActionListener<IndexResponse>() {
                @Override
                public void onResponse(IndexResponse response) {
                    try {
                        XContentBuilder builder = jsonBuilder();
                        builder.startObject().field("ok", true).field("id", response.id()).endObject();
                        channel.sendResponse(TYPE, builder);
                        // receive outstanding messages
                        sync(subscriberId, topic, channel.getChannel());
                    } catch (Exception e) {
                        onFailure(e);
                    }
                }

                @Override
                public void onFailure(Throwable e) {
                    logger.error("error while processing subscribe request", e);
                    try {
                        channel.sendResponse(TYPE, e);
                    } catch (IOException ex) {
                        logger.error("error while sending error response", ex);
                    }
                }
            });
        } catch (Exception e) {
            logger.error("exception while processing subscribe request", e);
            try {
                channel.sendResponse(TYPE, e);
            } catch (IOException e1) {
                logger.error("exception while sending exception response", e1);
            }
        }
    }

    private XContentBuilder createSubscriberMessage(String topic, InteractiveChannel channel) {
        Integer channelId = channel.getChannel().getId();
        String localAddress = channel.getChannel().getLocalAddress().toString();
        String remoteAddress = channel.getChannel().getRemoteAddress().toString();
        try {
            return jsonBuilder()
                    .startObject()
                    .field("topic", topic)
                    .startObject("subscriber")
                    .startObject("channel")
                    .field("id", channelId)
                    .field("localAddress", localAddress)
                    .field("remoteAddress", remoteAddress)
                    .endObject()
                    .endObject()
                    .endObject();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Synchronize the subscriber with the current messages.
     * 
     * @param subscriberId
     * @param topic
     * @param channel
     * @throws IOException 
     */
    private void sync(final String subscriberId, final String topic, final Channel channel) throws IOException {
        Long lastSeen = service.checkpointedAt(subscriberId);
        Long topicSeen = service.checkpointedAt(topic);
        // if client appearance is later than topic, do not search for any messages
        if (lastSeen == null || topicSeen == null || lastSeen >= topicSeen) {
            return;
        }
        // message sync - update subscriber with the current timestamp
        service.checkpoint(subscriberId);
        service.flushCheckpoint();        
        // there are unreceived messages, get all outstanding messages since last seen
        QueryBuilder queryBuilder = termQuery("topic", topic);
        NumericRangeFilterBuilder filterBuilder = numericRangeFilter("timestamp").gte(lastSeen);
        SearchResponse searchResponse = client.prepareSearch()
                .setIndices(pubSubIndexName)
                .setTypes("publish")
                .setSearchType(SearchType.SCAN)
                .setScroll(scrollTimeout)
                .setQuery(queryBuilder)
                .setFilter(filterBuilder)
                .addField("data")
                .addField("timestamp")
                .setSize(scrollSize)
                .execute().actionGet();
        boolean failed = searchResponse.failedShards() > 0 || searchResponse.timedOut();
        if (failed) {
            logger.error("searching for messages for topic {} failed: failed shards={} timeout={}",
                    topic, searchResponse.failedShards(), searchResponse.timedOut());
            return;
        }
        // look for messages
        long totalHits = searchResponse.getHits().getTotalHits();
        boolean zero = totalHits == 0L;
        if (zero) {
            return;
        }
        // slurp in all outstanding messages
        while (true) {
            searchResponse = client.prepareSearchScroll(searchResponse.getScrollId())
                    .setScroll(scrollTimeout)
                    .execute().actionGet();
            for (SearchHit hit : searchResponse.hits()) {
                long timestamp = hit.field("timestamp").getValue();
                Map<String, Object> data =  hit.field("data").getValue();
                channel.write(new NettyInteractiveResponse("message", createPublishMessage(timestamp, data)).response());
            }
            if (searchResponse.hits().hits().length == 0) {
                break;
            }
        }
    }
    
    private XContentBuilder createPublishMessage(long timestamp, Map<String,Object> data) {
        try {
            return jsonBuilder().startObject()
                    .field("timestamp", timestamp)
                    .field("data", data)
                    .endObject();
        } catch (IOException e) {
            return null;
        }
    }    
}
