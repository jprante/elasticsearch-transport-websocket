package org.xbib.elasticsearch.websocket;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.xbib.elasticsearch.http.netty.NettyInteractiveChannel;
import org.xbib.elasticsearch.http.netty.NettyInteractiveRequest;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * The InteractiveController controls the presence of websocket connections
 * and the flow of websocket frames for interactive use.
 */
public class InteractiveController extends AbstractLifecycleComponent<InteractiveController> {

    private final ESLogger logger = ESLoggerFactory.getLogger(InteractiveController.class.getSimpleName());

    private final HashMap<String, InteractiveHandler> handlers = new HashMap();

    @Inject
    public InteractiveController(Settings settings) {
        super(settings);
    }

    @Override
    protected void doStart() throws ElasticsearchException {
    }

    @Override
    protected void doStop() throws ElasticsearchException {
    }

    @Override
    protected void doClose() throws ElasticsearchException {
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
        Channel channel = context.getChannel();
        if (frame instanceof TextWebSocketFrame) {
            text((TextWebSocketFrame) frame, channel);
        } else if (handshaker != null && frame instanceof CloseWebSocketFrame) {
            handshaker.close(context.getChannel(), (CloseWebSocketFrame) frame);
            presence(Presence.DISCONNECTED, null, channel);
        } else if (frame instanceof PingWebSocketFrame) {
            channel.write(new PongWebSocketFrame(frame.getBinaryData()));
        }
    }

    private void text(TextWebSocketFrame frame, Channel channel) {
        Map<String, Object> map = parse(frame.getBinaryData().toString(Charset.forName("UTF-8")));
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
            error("missing handler for type: " + type, channel);
            return;
        }
        Map<String, Object> data = (Map<String, Object>) map.get("data");
        handlers.get(type).handleRequest(new NettyInteractiveRequest(data),
                new NettyInteractiveChannel(channel));
    }

    private Map<String, Object> parse(String source) {
        Map<String, Object> map = null;
        XContentParser parser = null;
        try {
            parser = XContentFactory.xContent(source).createParser(source);
            map = parser.map();
        } catch (Exception e) {
            logger.error("unable to parse: {}", source);
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
        return map;
    }

    private void error(String message, Channel channel) {
        String text = "{\"ok\":false,\"error\":\"" + message + "\"}";
        TextWebSocketFrame frame = new TextWebSocketFrame(text);
        channel.write(frame);
    }
}
