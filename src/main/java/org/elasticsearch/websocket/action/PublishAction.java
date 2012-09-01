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
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.websocket.InteractiveChannel;
import org.elasticsearch.websocket.InteractiveController;
import org.elasticsearch.websocket.InteractiveRequest;
import org.elasticsearch.websocket.http.HttpServerTransport;

/**
 * Publish action
 * 
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class PublishAction extends PublishSubscribe {

    protected final static String TYPE = "publish";    

    @Inject
    public PublishAction(Settings settings, 
            Client client,
            HttpServerTransport transport,
            InteractiveController controller,
            Checkpointer service) {
        super(settings, client, transport, controller, service);
        controller.registerHandler(TYPE, this);
    }

    @Override
    public void handleRequest(final InteractiveRequest request, final InteractiveChannel channel) {
        String topic = request.hasParam("topic") ? request.paramAsString("topic") : "*";
        try {
            // advertise phase - save message in the index (for disconnected subscribers)
            final XContentBuilder messageBuilder = createPublishMessage(request);
            final XContentBuilder responseBuilder = jsonBuilder().startObject();
            IndexResponse indexResponse = client.prepareIndex()
                    .setIndex(pubSubIndexName)
                    .setType(TYPE)
                    .setSource(messageBuilder)
                    .setRefresh(request.paramAsBoolean("refresh", true))
                    .execute().actionGet();
            responseBuilder.field("id", indexResponse.id());            
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
            boolean failed = searchResponse.failedShards() > 0 || searchResponse.timedOut();
            if (failed) {
                logger.error("searching for subscribers for topic {} failed: failed shards={} timeout={}", 
                        topic,searchResponse.failedShards(), searchResponse.timedOut());
                responseBuilder.field("subscribers", 0).field("failed",true);
                channel.sendResponse(TYPE, responseBuilder.endObject());
                responseBuilder.close();
                return;
            }
            // look for subscribers
            long totalHits = searchResponse.getHits().getTotalHits();
            boolean zero = totalHits == 0L;
            if (zero) {
                responseBuilder.field("subscribers", 0).field("failed",false);
                channel.sendResponse(TYPE, responseBuilder.endObject());
                responseBuilder.close();
                return;                
            }            
            // report the total number of subscribers online to the publisher
            responseBuilder.field("subscribers", totalHits);
            channel.sendResponse(TYPE, responseBuilder.endObject());
            messageBuilder.close();
            responseBuilder.close();
            // checkpoint topic
            service.checkpoint(topic);
            // push phase - write the message to the subscribers. We have 60 seconds per 100 subscribers.
            while (true) {
                searchResponse = client.prepareSearchScroll(searchResponse.getScrollId())
                        .setScroll(new TimeValue(60000))                        
                        .execute().actionGet();
                for (SearchHit hit : searchResponse.hits()) {
                    // for message sync - update all subscribers with the current timestamp
                    service.checkpoint(hit.getId());
                    // find node address and channel ID
                    SearchHitField channelField = hit.field("subscriber.channel");
                    Map<String,Object> channelfieldMap = channelField.getValue();
                    String nodeAddress = (String)channelfieldMap.get("localAddress");
                    Integer id = (Integer) channelfieldMap.get("id");
                    // forward to node
                    transport.forward(nodeAddress, id, messageBuilder);
                }
                if (searchResponse.hits().hits().length == 0) {
                    break;
                }
            }
            service.flushCheckpoint();
        } catch (Exception e) {
            logger.error("exception while processing publish request", e);
            try {
                channel.sendResponse(TYPE, e);
            } catch (IOException e1) {
                logger.error("exception while sending exception response", e1);
            }
        }
    }

    private XContentBuilder createPublishMessage(InteractiveRequest request) {
        try {
            return jsonBuilder().startObject()
                    .field("timestamp", request.paramAsLong("timestamp", System.currentTimeMillis()))
                    .field("data", request.asMap())
                    .endObject();
        } catch (IOException e) {
            return null;
        }
    }

}
