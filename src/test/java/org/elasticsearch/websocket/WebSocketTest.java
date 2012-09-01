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

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.websocket.client.WebSocketActionListener;
import org.elasticsearch.websocket.client.WebSocketClient;
import org.elasticsearch.websocket.client.WebSocketClientFactory;
import org.elasticsearch.websocket.http.netty.client.NettyWebSocketClientFactory;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.testng.annotations.Test;

public class WebSocketTest {

    private final static ESLogger logger = ESLoggerFactory.getLogger("test");

    /**
     * Primitive websocket client, just send "Hello World"
     */
    @Test
    public void helloWorld() {
        try {
            WebSocketClientFactory clientFactory = new NettyWebSocketClientFactory();
            WebSocketClient client = clientFactory.newClient(new URI("ws://localhost:9400/websocket"),
                    new WebSocketActionListener() {
                        @Override
                        public void onConnect(WebSocketClient client) {
                            logger.info("web socket connected");
                            client.send(new TextWebSocketFrame("Hello World"));
                        }

                        @Override
                        public void onDisconnect(WebSocketClient client) {
                            logger.info("web socket disconnected");
                        }

                        @Override
                        public void onMessage(WebSocketClient client, WebSocketFrame frame) {
                            logger.info("frame received: " + frame);
                        }

                        @Override
                        public void onError(Throwable t) {
                            logger.error(t.getMessage(), t);
                        }
                    });
            client.connect().await(1000, TimeUnit.MILLISECONDS);
            Thread.sleep(1000);
            client.send(new CloseWebSocketFrame());
            Thread.sleep(1000);
            client.disconnect();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
