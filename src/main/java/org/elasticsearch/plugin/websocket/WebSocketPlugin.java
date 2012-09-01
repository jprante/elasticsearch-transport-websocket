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
package org.elasticsearch.plugin.websocket;

import java.util.Collection;
import static org.elasticsearch.common.collect.Lists.newArrayList;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.websocket.WebSocketModule;
import org.elasticsearch.websocket.action.Checkpointer;
import org.elasticsearch.websocket.http.HttpServer;
import org.elasticsearch.websocket.http.HttpServerModule;
import org.elasticsearch.websocket.rest.action.RestPublishAction;
import org.elasticsearch.websocket.rest.action.RestUnsubscribeAction;

/**
 * WebSocket plugin
 * 
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class WebSocketPlugin extends AbstractPlugin {

    private final Settings settings;

    public WebSocketPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "transport-websocket";
    }

    @Override
    public String description() {
        return "WebSocket transport plugin";
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = newArrayList();
        if (settings.getAsBoolean("websocket.enabled", true)) {
            modules.add(HttpServerModule.class);
            modules.add(WebSocketModule.class);
        }
        return modules;
    }

   @Override public Collection<Class<? extends LifecycleComponent>> services() {
        Collection<Class<? extends LifecycleComponent>> services = newArrayList();
        if (settings.getAsBoolean("websocket.enabled", true)) {
            services.add(HttpServer.class);
            services.add(Checkpointer.class);
        }
        return services;
    }    
  
    public void onModule(RestModule module) {
        module.addRestAction(RestPublishAction.class);
        module.addRestAction(RestUnsubscribeAction.class);
    }

}
