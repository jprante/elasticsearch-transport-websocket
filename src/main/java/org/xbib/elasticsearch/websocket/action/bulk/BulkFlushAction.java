
package org.xbib.elasticsearch.websocket.action.bulk;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.xbib.elasticsearch.websocket.InteractiveChannel;
import org.xbib.elasticsearch.websocket.InteractiveController;
import org.xbib.elasticsearch.websocket.InteractiveRequest;

/**
 * Bulk flush action. This action forces bulk requests to get
 * sent to the cluster.
 */
public class BulkFlushAction extends BulkHandler {

    private final static String TYPE = "flush";

    @Inject
    public BulkFlushAction(Settings settings,
            Client client, InteractiveController controller) {
        super(settings, client);
        controller.registerHandler(TYPE, this);
    }

    @Override
    public void handleRequest(final InteractiveRequest request, final InteractiveChannel channel) {
        this.flush();
    }
}
