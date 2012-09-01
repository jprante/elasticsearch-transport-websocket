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
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.websocket.InteractiveChannel;
import org.elasticsearch.websocket.InteractiveController;
import org.elasticsearch.websocket.InteractiveRequest;

/**
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class BulkIndexAction extends BulkHandler {

    private final String TYPE = "index";

    @Inject
    public BulkIndexAction(Settings settings,
            Client client, InteractiveController controller) {
        super(settings, client);
        controller.registerHandler(TYPE, this);
    }

    @Override
    public void handleRequest(final InteractiveRequest request, final InteractiveChannel channel) {
        String index = request.paramAsString("index");
        String type = request.paramAsString("type");
        String id = request.paramAsString("id");
        try {
            if (index == null) {
                channel.sendResponse(TYPE, new IllegalArgumentException("index is null"));
                return;
            }
            if (type == null) {
                channel.sendResponse(TYPE, new IllegalArgumentException("type is null"));
                return;
            }
            if (id == null) {
                channel.sendResponse(TYPE, new IllegalArgumentException("id is null"));
                return;
            }
            IndexRequest indexRequest = Requests.indexRequest(index).type(type).id(id)
                .source((Map<String,Object>)request.asMap().get("data"));
            add(indexRequest, channel);
        } catch (IOException ex) {
            try {
                channel.sendResponse(TYPE, ex);
            } catch (IOException ex1) {
                logger.error("error while sending exception");
            }
        }
    }    
}
