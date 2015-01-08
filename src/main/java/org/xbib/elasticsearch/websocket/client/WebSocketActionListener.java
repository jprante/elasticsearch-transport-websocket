package org.xbib.elasticsearch.websocket.client;

import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.io.IOException;

/**
 * Listening to WebSocket actions.
 *
 * @param <C> the WebSocketClient class parameter
 * @param <F> the WebSocketFrame class parameter
 */
public interface WebSocketActionListener<C extends WebSocketClient, F extends WebSocketFrame> {

    /**
     * Called when the client is connected to the server
     *
     * @param client Current client used to connect
     * @throws java.io.IOException if this method fails
     */
    void onConnect(C client) throws IOException;

    /**
     * Called when the client got disconnected from the server
     *
     * @param client Current client that was disconnected
     * @throws java.io.IOException if this method fails
     */
    void onDisconnect(C client) throws IOException;

    /**
     * Called when a message arrives from the server
     *
     * @param client the connected client
     * @param frame  the data received from server
     * @throws java.io.IOException if this method fails
     */
    void onMessage(C client, F frame) throws IOException;

    /**
     * Called when an unhandled errors occurs.
     *
     * @param t The causing error
     * @throws java.io.IOException if this method fails
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
