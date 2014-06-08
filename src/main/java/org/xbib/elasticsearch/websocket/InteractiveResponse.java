package org.xbib.elasticsearch.websocket;

import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.io.IOException;

/**
 * The InteractiveResponse can serve frames to websocket connections.
 */
public interface InteractiveResponse {

    String type();

    TextWebSocketFrame response() throws IOException;
}
