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

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.websocket.BaseInteractiveHandler;
import org.elasticsearch.websocket.InteractiveController;
import org.elasticsearch.websocket.http.HttpServerTransport;

/**
 * Base class for Publish/Subscribe actions
 * 
 * @author Jörg Prante <joergprante@gmail.com>
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
