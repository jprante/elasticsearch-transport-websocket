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
package org.elasticsearch.websocket.rest.action;

import java.io.IOException;
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
import static org.elasticsearch.rest.RestStatus.BAD_REQUEST;
import static org.elasticsearch.rest.RestStatus.OK;
import org.elasticsearch.rest.XContentRestResponse;
import org.elasticsearch.rest.XContentThrowableRestResponse;
import static org.elasticsearch.rest.action.support.RestXContentBuilder.restContentBuilder;
import org.elasticsearch.websocket.action.PubSubIndexName;

/**
 * Unsubscribe action for REST
 * 
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class RestUnsubscribeAction extends BaseRestHandler {

    private final String pubSubIndexName;

    @Inject
    public RestUnsubscribeAction(Settings settings, Client client, 
            RestController restController) {
        super(settings, client);
        this.pubSubIndexName = PubSubIndexName.Conf.indexName(settings);        
        restController.registerHandler(RestRequest.Method.GET, "/_unsubscribe", this);
        restController.registerHandler(RestRequest.Method.POST, "/_unsubscribe", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
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
                        builder.startObject().field("ok", true).field("id", response.id()).endObject();
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
