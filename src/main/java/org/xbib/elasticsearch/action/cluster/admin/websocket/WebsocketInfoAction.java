
package org.xbib.elasticsearch.action.cluster.admin.websocket;

import org.elasticsearch.action.admin.cluster.ClusterAction;
import org.elasticsearch.client.ClusterAdminClient;

/**
 */
public class WebsocketInfoAction extends ClusterAction<WebsocketInfoRequest, WebsocketInfoResponse, WebsocketInfoRequestBuilder> {

    public static final WebsocketInfoAction INSTANCE = new WebsocketInfoAction();
    public static final String NAME = "org.xbib.elasticsearch.action.websocket.info";

    private WebsocketInfoAction() {
        super(NAME);
    }

    @Override
    public WebsocketInfoResponse newResponse() {
        return new WebsocketInfoResponse();
    }

    @Override
    public WebsocketInfoRequestBuilder newRequestBuilder(ClusterAdminClient client) {
        return new WebsocketInfoRequestBuilder(client);
    }
}
