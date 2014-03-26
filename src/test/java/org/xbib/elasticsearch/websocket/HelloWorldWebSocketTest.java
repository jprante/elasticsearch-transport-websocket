
package org.xbib.elasticsearch.websocket;

import java.util.concurrent.TimeUnit;

import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.junit.Test;
import org.xbib.elasticsearch.websocket.client.WebSocketActionListener;
import org.xbib.elasticsearch.websocket.client.WebSocketClient;
import org.xbib.elasticsearch.websocket.client.WebSocketClientFactory;
import org.xbib.elasticsearch.websocket.helper.AbstractNodeTestHelper;
import org.xbib.elasticsearch.websocket.http.netty.client.NettyWebSocketClientFactory;


public class HelloWorldWebSocketTest extends AbstractNodeTestHelper {

    private final static ESLogger logger = ESLoggerFactory.getLogger("test");

    /**
     * Primitive websocket client, just send "Hello World"
     */
    @Test
    public void helloWorld() {
        try {
            WebSocketClientFactory clientFactory = new NettyWebSocketClientFactory();
            WebSocketClient client = clientFactory.newClient(getAddressOfNode("1"),
                    new WebSocketActionListener() {
                        @Override
                        public void onConnect(WebSocketClient client) {
                            logger.info("web socket connected");
                            String s = "{\"Hello\":\"World\"}";
                            client.send(new TextWebSocketFrame(s));
                            logger.info("sent " + s);
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
