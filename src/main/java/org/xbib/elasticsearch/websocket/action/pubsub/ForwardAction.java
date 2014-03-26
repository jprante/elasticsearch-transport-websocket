
package org.xbib.elasticsearch.websocket.action.pubsub;

import java.io.IOException;
import java.util.Map;

import org.jboss.netty.channel.Channel;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

import org.xbib.elasticsearch.websocket.BaseInteractiveHandler;
import org.xbib.elasticsearch.websocket.InteractiveChannel;
import org.xbib.elasticsearch.websocket.InteractiveController;
import org.xbib.elasticsearch.websocket.InteractiveRequest;
import org.xbib.elasticsearch.websocket.http.HttpServerTransport;
import org.xbib.elasticsearch.websocket.http.netty.NettyInteractiveResponse;

/**
 * Forwarding a message to a destination.
 */
public class ForwardAction extends BaseInteractiveHandler {

    private final String TYPE = "forward";
    private final HttpServerTransport transport;

    @Inject
    public ForwardAction(Settings settings,
            Client client,
            HttpServerTransport transport,
            InteractiveController controller) {
        super(settings, client);
        this.transport = transport;
        controller.registerHandler(TYPE, this);
    }

    @Override
    public void handleRequest(final InteractiveRequest request, final InteractiveChannel channel) {
        Map<String, Object> m = request.asMap();
        Integer id = (Integer) m.get("channel");
        Channel ch = transport.channel(id);
        try {
            if (ch != null) {
                ch.write(new NettyInteractiveResponse("message", (Map<String, Object>) m.get("message")).response());
                // don't send a success message back to the channel
            } else {
                // delivery failed, channel not present
                channel.sendResponse(TYPE, new IOException("channel " + id + " gone"));
            }
        } catch (IOException ex) {
            logger.error("error while delivering forward message {}: {}", m, ex);
        }
    }
}
