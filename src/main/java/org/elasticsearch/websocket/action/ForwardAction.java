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
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.websocket.BaseInteractiveHandler;
import org.elasticsearch.websocket.InteractiveChannel;
import org.elasticsearch.websocket.InteractiveController;
import org.elasticsearch.websocket.InteractiveRequest;
import org.elasticsearch.websocket.http.HttpServerTransport;
import org.elasticsearch.websocket.http.netty.NettyInteractiveResponse;
import org.jboss.netty.channel.Channel;

/**
 * Forwarding a message to a destination.
 * 
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class ForwardAction extends BaseInteractiveHandler {

    private final String TYPE = "forward";
    private final HttpServerTransport transport;

    @Inject
    public ForwardAction(Settings settings,
            Client client,
            HttpServerTransport transport,
            InteractiveController controller) {
        super(settings, client);
        this.transport = transport;
        controller.registerHandler(TYPE, this);
    }

    @Override
    public void handleRequest(final InteractiveRequest request, final InteractiveChannel channel) {
        Map<String, Object> m = request.asMap();
        Integer id = (Integer) m.get("channel");
        Channel ch = transport.channel(id);
        try {
            if (ch != null) {
                ch.write(new NettyInteractiveResponse("message", (Map<String, Object>) m.get("message")).response());
                // don't send a success message back to the channel
            } else {
                // delivery failed, channel not present
                channel.sendResponse(TYPE, new IOException("channel " + id + " gone"));
            }
        } catch (IOException ex) {
            logger.error("error while delivering forward message {}: {}", m, ex);
        }
    }
}
