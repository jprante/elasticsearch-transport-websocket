
package org.xbib.elasticsearch.websocket;

import java.io.IOException;
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
import org.xbib.elasticsearch.websocket.client.WebSocketClientRequest;
import org.xbib.elasticsearch.websocket.helper.AbstractNodeTestHelper;
import org.xbib.elasticsearch.http.netty.client.NettyWebSocketClientFactory;

public class PublishSubscribeRequestTest extends AbstractNodeTestHelper {
    
    private final static ESLogger logger = ESLoggerFactory.getLogger("test");    
    
    public void testOneClient() {
        try {
            final String subscriberId = "oneclient";
            final String topic = "oneclienttest";
            final WebSocketClientFactory clientFactory = new NettyWebSocketClientFactory();
            
            final WebSocketClientRequest subscribe = clientFactory.newRequest()
                    .type("subscribe").data(jsonBuilder().startObject()
                    .field("subscriber", "singleclient")
                    .field("topic", topic).endObject());
            
            final WebSocketClientRequest publish = clientFactory.newRequest()
                    .type("publish").data(jsonBuilder().startObject()
                    .field("message", "Hello World")
                    .field("topic", topic).endObject());
            
            WebSocketClient client = clientFactory.newClient(getAddressOfNode("1"),
                    new WebSocketActionListener.Adapter() {
                        @Override
                        public void onConnect(WebSocketClient client) throws IOException {
                                logger.info("sending subscribe command, channel = {}", client.channel());
                                subscribe.send(client);
                                logger.info("sending publish command (to ourselves), channel = {}", client.channel());
                                publish.send(client);
                        }

                        @Override
                        public void onMessage(WebSocketClient client, WebSocketFrame frame) {
                            logger.info("frame received: " + frame);
                        }
                    });
            client.connect().await(1000, TimeUnit.MILLISECONDS);            
            Thread.sleep(1000);
            client.send(new CloseWebSocketFrame());
            Thread.sleep(1000);
            client.disconnect();
            
            clientFactory.shutdown();
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }        
    }

    @Test
    public void testTwoClients() {
        try {
            final String subscriberId = "twoclients";
            final String topic = "twoclienttest";
            final WebSocketClientFactory clientFactory = new NettyWebSocketClientFactory();

            final WebSocketClientRequest subscribe = clientFactory.newRequest()
                    .type("subscribe").data(jsonBuilder().startObject()
                    .field("subscriber", "doubleclient")
                    .field("topic", topic).endObject());
            
            final WebSocketClientRequest publish = clientFactory.newRequest()
                    .type("publish").data(jsonBuilder().startObject()
                    .field("message", "Hi there, I'm another client")
                    .field("topic", topic).endObject());
            
            // open first client
            WebSocketClient subscribingClient = clientFactory.newClient(getAddressOfNode("1"),
                    new WebSocketActionListener.Adapter() {
                        @Override
                        public void onConnect(WebSocketClient client) {
                            try {
                                logger.info("sending subscribe command, channel {}", client.channel());
                                subscribe.send(client);
                            } catch (Exception e) {
                            }
                        }

                        @Override
                        public void onMessage(WebSocketClient client, WebSocketFrame frame) {
                            logger.info("frame {} received for subscribing client {}", frame, client.channel());
                        }

                    });
            
            // open two client
            WebSocketClient publishingClient = clientFactory.newClient(getAddressOfNode("1"),
                    new WebSocketActionListener.Adapter() {
                        @Override
                        public void onConnect(WebSocketClient client) {
                            try {
                                logger.info("sending publish command, channel = {}", client.channel());
                                publish.send(client);
                            } catch (Exception e) {
                            }
                        }

                        @Override
                        public void onMessage(WebSocketClient client, WebSocketFrame frame) {
                            logger.info("frame {} received for publishing client {}", frame, client.channel());
                        }

                    });
            
            // connect both clients to node
            subscribingClient.connect().await(1000, TimeUnit.MILLISECONDS);            
            publishingClient.connect().await(1000, TimeUnit.MILLISECONDS);            
            
            // wait for publish/subscribe
            Thread.sleep(1000);
            
            // close first client
            publishingClient.send(new CloseWebSocketFrame());
            publishingClient.disconnect();

            // close second client
            subscribingClient.send(new CloseWebSocketFrame());
            subscribingClient.disconnect();
            
            Thread.sleep(1000);
            
            clientFactory.shutdown();
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }        
    }

}
