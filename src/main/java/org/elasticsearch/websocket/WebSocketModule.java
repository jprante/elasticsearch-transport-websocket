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
import org.elasticsearch.common.settings.Settings;

/**
 * The WebSocketModule sets up the InteractiveController
 * and the InteractiveActionModule.
 * 
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class WebSocketModule extends AbstractModule {

    private final Settings settings;
    private List<Class<? extends BaseInteractiveHandler>> webSocketActions = Lists.newArrayList();

    public WebSocketModule(Settings settings) {
        this.settings = settings;
    }

    public void addInteractiveAction(Class<? extends BaseInteractiveHandler> action) {
        webSocketActions.add(action);
    }

    @Override
    protected void configure() {
        bind(InteractiveController.class).asEagerSingleton();
        new InteractiveActionModule(webSocketActions).configure(binder());    
    }

}
