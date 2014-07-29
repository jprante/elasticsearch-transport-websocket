package org.xbib.elasticsearch.action.cluster.admin.websocket;

import org.elasticsearch.action.support.nodes.NodesOperationResponse;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.List;

public class WebsocketInfoResponse extends NodesOperationResponse<WebsocketInfo> implements ToXContent {

    public WebsocketInfoResponse() {
    }

    public WebsocketInfoResponse(ClusterName clusterName, List<WebsocketInfo> nodes) {
        super(clusterName, nodes.toArray(new WebsocketInfo[nodes.size()]));
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        nodes = new WebsocketInfo[in.readVInt()];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = WebsocketInfo.readInfo(in);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(nodes.length);
        for (WebsocketInfo node : nodes) {
            node.writeTo(out);
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field("cluster_name", getClusterName().value(), XContentBuilder.FieldCaseConversion.NONE);

        builder.startObject("nodes");
        for (WebsocketInfo nodeInfo : this) {
            builder.startObject(nodeInfo.getNode().id(), XContentBuilder.FieldCaseConversion.NONE);
            builder.field("name", nodeInfo.getNode().name(), XContentBuilder.FieldCaseConversion.NONE);
            builder.field("transport_address", nodeInfo.getNode().address().toString());
            builder.field("host", nodeInfo.getNode().getHostName(), XContentBuilder.FieldCaseConversion.NONE);
            builder.field("ip", nodeInfo.getNode().getHostAddress(), XContentBuilder.FieldCaseConversion.NONE);
            builder.field("websocket_address", nodeInfo.getAddress(), XContentBuilder.FieldCaseConversion.NONE);
            builder.endObject();
        }
        builder.endObject();
        return builder;
    }

    @Override
    public String toString() {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().prettyPrint();
            builder.startObject();
            toXContent(builder, EMPTY_PARAMS);
            builder.endObject();
            return builder.string();
        } catch (IOException e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }
}
