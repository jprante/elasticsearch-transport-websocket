
package org.xbib.elasticsearch.websocket.client;

import java.io.IOException;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 *  Listening to WebSocket actions.
 * 
 * @param <C>
 * @param <F> 
 */
public interface WebSocketActionListener<C extends WebSocketClient, F extends WebSocketFrame> {

    /**
     * Called when the client is connected to the server
     *
     * @param client Current client used to connect
     */
    void onConnect(C client) throws IOException;

    /**
     * Called when the client got disconnected from the server
     *
     * @param client Current client that was disconnected
     */
    void onDisconnect(C client) throws IOException;

    /**
     * Called when a message arrives from the server
     *
     * @param client the connected client
     * @param frame the data received from server
     */
    void onMessage(C client, F frame) throws IOException;

    /**
     * Called when an unhandled errors occurs.
     *
     * @param t The causing error
     */
    void onError(Throwable t) throws IOException;

    class Adapter implements WebSocketActionListener {

        @Override
        public void onConnect(WebSocketClient client) throws IOException {
        }

        @Override
        public void onDisconnect(WebSocketClient client) throws IOException {
        }

        @Override
        public void onMessage(WebSocketClient client, WebSocketFrame frame) throws IOException {
        }

        @Override
        public void onError(Throwable t) throws IOException {
        }
        
    }
    
}
