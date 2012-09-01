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

package org.elasticsearch.websocket;

import java.util.List;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.websocket.action.BulkDeleteAction;
import org.elasticsearch.websocket.action.BulkFlushAction;
import org.elasticsearch.websocket.action.BulkIndexAction;
import org.elasticsearch.websocket.action.ForwardAction;
import org.elasticsearch.websocket.action.PublishAction;
import org.elasticsearch.websocket.action.SubscribeAction;
import org.elasticsearch.websocket.action.UnsubscribeAction;

/**
 * The InteractiveActionModule binds all WebSocket actions.
 * 
 * @author Jörg Prante <joergprante@gmail.com>
 */

public class InteractiveActionModule extends AbstractModule {

    private List<Class<? extends BaseInteractiveHandler>> websocketActions = Lists.newArrayList();

    public InteractiveActionModule(List<Class<? extends BaseInteractiveHandler>> websocketActions) {
        this.websocketActions = websocketActions;
    }

    @Override
    protected void configure() {
        for (Class<? extends BaseInteractiveHandler> websocketAction : websocketActions) {
            bind(websocketAction).asEagerSingleton();
        }
        bind(PublishAction.class).asEagerSingleton();
        bind(SubscribeAction.class).asEagerSingleton();
        bind(UnsubscribeAction.class).asEagerSingleton();
        bind(ForwardAction.class).asEagerSingleton();
        bind(BulkDeleteAction.class).asEagerSingleton();
        bind(BulkIndexAction.class).asEagerSingleton();
        bind(BulkFlushAction.class).asEagerSingleton();
    }
}
