package org.xbib.elasticsearch.action.websocket.bulk;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.xbib.elasticsearch.websocket.InteractiveChannel;
import org.xbib.elasticsearch.websocket.InteractiveController;
import org.xbib.elasticsearch.websocket.InteractiveRequest;

import java.io.IOException;

/**
 * Bulk delete action
 */
public class BulkDeleteAction extends BulkHandler {

    private final static String TYPE = "delete";

    @Inject
    public BulkDeleteAction(Settings settings,
                            Client client, InteractiveController controller) {
        super(settings, client);
        controller.registerHandler(TYPE, this);
    }

    @Override
    public void handleRequest(final InteractiveRequest request, final InteractiveChannel channel) {
        String index = request.paramAsString("index");
        String type = request.paramAsString("type");
        String id = request.paramAsString("id");
        try {
            if (index == null) {
                channel.sendResponse(TYPE, new IllegalArgumentException("index is null"));
                return;
            }
            if (type == null) {
                channel.sendResponse(TYPE, new IllegalArgumentException("type is null"));
                return;
            }
            if (id == null) {
                channel.sendResponse(TYPE, new IllegalArgumentException("id is null"));
                return;
            }
            DeleteRequest deleteRequest = Requests.deleteRequest(index).type(type).id(id);
            add(deleteRequest, channel);
        } catch (IOException ex) {
            try {
                channel.sendResponse(TYPE, ex);
            } catch (IOException ex1) {
                logger.error("error while sending exception");
            }
        }
    }
}
