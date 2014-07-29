package org.xbib.elasticsearch.action.cluster.admin.websocket;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.nodes.NodesOperationRequestBuilder;
import org.elasticsearch.client.ClusterAdminClient;

public class WebsocketInfoRequestBuilder extends NodesOperationRequestBuilder<WebsocketInfoRequest, WebsocketInfoResponse, WebsocketInfoRequestBuilder> {

    public WebsocketInfoRequestBuilder(ClusterAdminClient clusterClient) {
        super(clusterClient, new WebsocketInfoRequest());
    }

    @Override
    protected void doExecute(ActionListener<WebsocketInfoResponse> listener) {
        client.execute(WebsocketInfoAction.INSTANCE, request, listener);
    }
}
