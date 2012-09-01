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

import java.util.HashMap;
import java.util.Map;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.websocket.http.netty.NettyInteractiveChannel;
import org.elasticsearch.websocket.http.netty.NettyInteractiveRequest;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;

/**
 * The InteractiveController controls the presence of websocket connections
 * and the flow of websocket frames for interactive use.
 * 
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class InteractiveController extends AbstractLifecycleComponent<InteractiveController> {

    private final HashMap<String, InteractiveHandler> handlers = new HashMap();

    @Inject
    public InteractiveController(Settings settings) {
        super(settings);
    }

    @Override
    protected void doStart() throws ElasticSearchException {
    }

    @Override
    protected void doStop() throws ElasticSearchException {
    }

    @Override
    protected void doClose() throws ElasticSearchException {
    }

    public void registerHandler(String type, InteractiveHandler handler) {
        handlers.put(type, handler);
    }

    public void presence(Presence presence, String topic, Channel channel) {
        if (logger.isDebugEnabled()) {
            logger.debug("presence: " + presence.name()
                    + " topic =" + topic
                    + " channel =" + channel);
        }
    }

    public void frame(WebSocketServerHandshaker handshaker, WebSocketFrame frame, ChannelHandlerContext context) {
        if (frame instanceof TextWebSocketFrame) {
            text((TextWebSocketFrame) frame, context.getChannel());
        } else if (handshaker != null && frame instanceof CloseWebSocketFrame) {
            handshaker.close(context.getChannel(), (CloseWebSocketFrame) frame);
            presence(Presence.DISCONNECTED, null, context.getChannel());
        } else if (frame instanceof PingWebSocketFrame) {
            context.getChannel().write(new PongWebSocketFrame(frame.getBinaryData()));
        }
    }

    private void text(TextWebSocketFrame frame, Channel channel) {
        Map<String, Object> map = parse(frame.getText());
        if (map == null) {
            error("invalid request", channel);
            return;
        }
        String type = (String) map.get("type");
        if (type == null) {
            error("no type found", channel);
            return;
        }
        if (!handlers.containsKey(type)) {
            error("missing handler for type " + type, channel);
            return;
        }
        Map<String, Object> data = (Map<String, Object>) map.get("data");
        handlers.get(type).handleRequest(new NettyInteractiveRequest(data), new NettyInteractiveChannel(channel));
    }

    private Map<String, Object> parse(String source) {
        Map<String, Object> map = null;
        try (XContentParser parser = XContentFactory.xContent(source).createParser(source)) {
            map = parser.map();
        } catch (Exception e) {
            logger.warn("unable to parse {}", source);
        }
        return map;
    }

    private void error(String message, Channel channel) {
        TextWebSocketFrame frame = new TextWebSocketFrame();
        String text = "{\"ok\":false,\"error\":\"" + message + "\"}";
        frame.setText(text);
        channel.write(frame);
    }
}
