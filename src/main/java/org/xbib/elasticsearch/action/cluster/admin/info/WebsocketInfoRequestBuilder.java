package org.xbib.elasticsearch.action.cluster.admin.info;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.nodes.NodesOperationRequestBuilder;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.internal.InternalClusterAdminClient;

public class WebsocketInfoRequestBuilder extends NodesOperationRequestBuilder<WebsocketInfoRequest, WebsocketInfoResponse, WebsocketInfoRequestBuilder> {

    public WebsocketInfoRequestBuilder(ClusterAdminClient clusterClient) {
        super((InternalClusterAdminClient) clusterClient, new WebsocketInfoRequest());
    }

    @Override
    protected void doExecute(ActionListener<WebsocketInfoResponse> listener) {
        ((ClusterAdminClient) client).execute(WebsocketInfoAction.INSTANCE, request, listener);
    }
}
