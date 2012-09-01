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
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.websocket.BaseInteractiveHandler;
import org.elasticsearch.websocket.InteractiveChannel;
import org.elasticsearch.websocket.InteractiveController;
import org.elasticsearch.websocket.InteractiveRequest;

/**
 * Unsubscribe action. It removes a subscription.
 * 
 * @author Jörg Prante <joergprante@gmail.com>
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
                        builder.startObject().field("ok", true).field("id", response.id()).endObject();
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
