package org.xbib.elasticsearch.action.cluster.admin.info;

import org.elasticsearch.action.support.nodes.NodeOperationResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.io.IOException;

public class WebsocketInfo extends NodeOperationResponse {

    private InetSocketTransportAddress address;

    WebsocketInfo() {
    }

    public WebsocketInfo(DiscoveryNode node, InetSocketTransportAddress address) {
        super(node);
        this.address = address;
    }

    public InetSocketTransportAddress getAddress() {
        return address;
    }

    public static WebsocketInfo readInfo(StreamInput in) throws IOException {
        WebsocketInfo info = new WebsocketInfo();
        info.readFrom(in);
        return info;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        address = InetSocketTransportAddress.readInetSocketTransportAddress(in);
    }

}
