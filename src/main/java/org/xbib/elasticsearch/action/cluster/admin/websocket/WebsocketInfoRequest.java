package org.xbib.elasticsearch.action.cluster.admin.websocket;

import org.elasticsearch.action.support.nodes.NodesOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

public class WebsocketInfoRequest extends NodesOperationRequest<WebsocketInfoRequest> {

    public WebsocketInfoRequest() {
    }

    public WebsocketInfoRequest(String... nodesIds) {
        super(nodesIds);
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
    }
}
