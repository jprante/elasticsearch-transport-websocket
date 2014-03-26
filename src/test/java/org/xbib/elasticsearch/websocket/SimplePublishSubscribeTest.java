
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

public class SimplePublishSubscribeTest extends AbstractNodeTestHelper {

    private final static ESLogger logger = ESLoggerFactory.getLogger("test");

    /**
     * In this test, we just use simple TextWebSocketFrame to send publish/subscribe requests.
     */
    @Test
    public void subscribeToOurselves() {
        try {
            WebSocketClientFactory clientFactory = new NettyWebSocketClientFactory();
            WebSocketClient client = clientFactory.newClient(getAddressOfNode("1"),
                    new WebSocketActionListener() {
                        @Override
                        public void onConnect(WebSocketClient client) {
                            try {
                                logger.info("sending subscribe command");
                                client.send(new TextWebSocketFrame("{\"type\":\"subscribe\",\"data\":{\"subscriber\":\"mypubsubdemo\",\"topic\":\"demo\"}}"));
                                Thread.sleep(500);
                                logger.info("sending publish command (to ourselves)");
                                client.send(new TextWebSocketFrame("{\"type\":\"publish\",\"data\":{\"message\":\"Hello World\",\"topic\":\"demo\"}}"));
                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                            }
                        }

                        @Override
                        public void onDisconnect(WebSocketClient client) {
                            logger.info("web socket disconnected");
                        }

                        @Override
                        public void onMessage(WebSocketClient client, WebSocketFrame frame) {
                            logger.info("frame received: {}", frame);
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
