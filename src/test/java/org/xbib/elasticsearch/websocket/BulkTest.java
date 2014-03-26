
package org.xbib.elasticsearch.websocket;

import java.util.concurrent.TimeUnit;

import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import org.junit.Test;
import org.xbib.elasticsearch.websocket.client.WebSocketActionListener;
import org.xbib.elasticsearch.websocket.client.WebSocketClient;
import org.xbib.elasticsearch.websocket.client.WebSocketClientFactory;
import org.xbib.elasticsearch.websocket.helper.AbstractNodeTestHelper;
import org.xbib.elasticsearch.websocket.http.netty.client.NettyWebSocketClientFactory;

public class BulkTest extends AbstractNodeTestHelper {

    private final static ESLogger logger = ESLoggerFactory.getLogger("test");

    @Test
    public void testBulk() {
        try {
            final WebSocketClientFactory clientFactory = new NettyWebSocketClientFactory();
            WebSocketClient client = clientFactory.newClient(
                    getAddressOfNode("1"),
                    new WebSocketActionListener.Adapter() {
                        @Override
                        public void onConnect(WebSocketClient client) {
                            try {
                                logger.info("sending some index requests (longer than a single bulk size)");
                                for (int i = 0; i < 250; i++) {
                                    clientFactory.indexRequest()
                                            .data(jsonBuilder()                                            
                                            .startObject()
                                            .field("index", "test")
                                            .field("type", "test")
                                            .field("id", Integer.toString(i))
                                            .startObject("data")
                                            .field("field1", "value" + i)
                                            .field("field2", "value" + i)
                                            .endObject()
                                            .endObject())
                                            .send(client);
                                }
                                // more bulks could be added here ...
                                logger.info("at the end, let us flush the bulk");
                                clientFactory.flushRequest().send(client);
                            } catch (Exception e) {
                                onError(e);
                            }
                        }

                        @Override
                        public void onDisconnect(WebSocketClient client) {
                            logger.info("disconnected");
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
            logger.info("closing bulk client");
            client.send(new CloseWebSocketFrame());
            Thread.sleep(1000);
            client.disconnect();
            clientFactory.shutdown();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }
}
