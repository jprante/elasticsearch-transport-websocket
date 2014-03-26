
package org.xbib.elasticsearch.websocket.http.netty;

import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.xbib.elasticsearch.websocket.InteractiveResponse;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Netty implementation of an interactive response
 */
public class NettyInteractiveResponse implements InteractiveResponse {

    private final String type;

    private final TextWebSocketFrame response;

    public NettyInteractiveResponse(String type, XContentBuilder builder) throws IOException {
        this.type = type;
        XContentBuilder responseBuilder = jsonBuilder()
                .startObject().field("success", true).field("type", type);
        if (builder != null) {
            responseBuilder.rawField("data", builder.bytes());
        }
        responseBuilder.endObject();
        this.response = new TextWebSocketFrame(responseBuilder.string());
    }

    public NettyInteractiveResponse(String type, Map<String, Object> map) throws IOException {
        this.type = type;
        XContentBuilder responseBuilder = jsonBuilder();
        responseBuilder.startObject().field("success", true).field("type", type).field("data", map).endObject();
        this.response = new TextWebSocketFrame(responseBuilder.string());
    }

    public NettyInteractiveResponse(String type, Throwable t) {
        this.type = type;
        this.response = new TextWebSocketFrame("{\"success\":false,\"type\":\"" + type + "\",\"error\":\"" + t.getMessage() + "\"");
    }

    @Override
    public String type() {
        return type;
    }

    /**
     * The response frame with content, ready for writing to a Channel
     *
     * @return a TextWebSocketFrame
     * @throws IOException
     */
    @Override
    public TextWebSocketFrame response() throws IOException {
        return response;
    }
}
