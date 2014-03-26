
package org.xbib.elasticsearch.websocket.http.netty.client;

import java.io.IOException;

/**
 * A WebSocket related exception
 */
public class NettyWebSocketException extends IOException {

    public NettyWebSocketException(String s) {
        super(s);
    }
}
