
package org.xbib.elasticsearch.websocket.http;

import java.io.IOException;
import java.io.Serializable;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.transport.BoundTransportAddress;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;


public class HttpInfo implements Streamable, Serializable, ToXContent {

    private BoundTransportAddress address;

    HttpInfo() {
    }

    public HttpInfo(BoundTransportAddress address) {
        this.address = address;
    }

    static final class Fields {
        static final XContentBuilderString HTTP = new XContentBuilderString("http");
        static final XContentBuilderString BOUND_ADDRESS = new XContentBuilderString("bound_address");
        static final XContentBuilderString PUBLISH_ADDRESS = new XContentBuilderString("publish_address");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(Fields.HTTP);
        builder.field(Fields.BOUND_ADDRESS, address.boundAddress().toString());
        builder.field(Fields.PUBLISH_ADDRESS, address.publishAddress().toString());
        builder.endObject();
        return builder;
    }

    public static HttpInfo readHttpInfo(StreamInput in) throws IOException {
        HttpInfo info = new HttpInfo();
        info.readFrom(in);
        return info;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        address = BoundTransportAddress.readBoundTransportAddress(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        address.writeTo(out);
    }

    public BoundTransportAddress address() {
        return address;
    }

    public BoundTransportAddress getAddress() {
        return address();
    }
}
