package org.xbib.elasticsearch.action.cluster.admin.info;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.support.nodes.NodeOperationRequest;
import org.elasticsearch.action.support.nodes.TransportNodesOperationAction;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.discovery.Discovery;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.xbib.elasticsearch.http.HttpServer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.elasticsearch.common.collect.Lists.newLinkedList;

public class TransportWebsocketInfoAction extends TransportNodesOperationAction<WebsocketInfoRequest, WebsocketInfoResponse, TransportWebsocketInfoAction.TransportWebsocketInfoRequest, WebsocketInfo> {

    private final Discovery discovery;

    private final HttpServer httpServer;

    @Inject
    public TransportWebsocketInfoAction(Settings settings, ClusterName clusterName, ThreadPool threadPool,
                                        ClusterService clusterService, TransportService transportService,
                                        Discovery discovery, HttpServer httpServer) {
        super(settings, clusterName, threadPool, clusterService, transportService);
        this.discovery = discovery;
        this.httpServer = httpServer;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected String transportAction() {
        return WebsocketInfoAction.NAME;
    }

    @Override
    protected WebsocketInfoResponse newResponse(WebsocketInfoRequest nodesInfoRequest, AtomicReferenceArray responses) {
        final List<WebsocketInfo> nodesInfos = newLinkedList();
        for (int i = 0; i < responses.length(); i++) {
            Object resp = responses.get(i);
            if (resp instanceof WebsocketInfo) {
                nodesInfos.add((WebsocketInfo) resp);
            }
        }
        return new WebsocketInfoResponse(clusterName, nodesInfos);
    }

    @Override
    protected WebsocketInfoRequest newRequest() {
        return new WebsocketInfoRequest();
    }

    @Override
    protected TransportWebsocketInfoRequest newNodeRequest() {
        return new TransportWebsocketInfoRequest();
    }

    @Override
    protected TransportWebsocketInfoRequest newNodeRequest(String nodeId, WebsocketInfoRequest request) {
        return new TransportWebsocketInfoRequest(nodeId, request);
    }

    @Override
    protected WebsocketInfo newNodeResponse() {
        return new WebsocketInfo();
    }

    @Override
    protected WebsocketInfo nodeOperation(TransportWebsocketInfoRequest nodeRequest) throws ElasticsearchException {
        return new WebsocketInfo(discovery.localNode(), (InetSocketTransportAddress)httpServer.address());
    }

    @Override
    protected boolean accumulateExceptions() {
        return false;
    }

    static class TransportWebsocketInfoRequest extends NodeOperationRequest {

        WebsocketInfoRequest request;

        TransportWebsocketInfoRequest() {
        }

        TransportWebsocketInfoRequest(String nodeId, WebsocketInfoRequest request) {
            super(request, nodeId);
            this.request = request;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            request = new WebsocketInfoRequest();
            request.readFrom(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            request.writeTo(out);
        }
    }
}
