package org.xbib.elasticsearch.http;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.transport.BoundTransportAddress;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;

import java.io.IOException;
import java.io.Serializable;


public class HttpInfo implements Streamable, Serializable, ToXContent {

    private BoundTransportAddress address;
    private long maxContentLength;

    HttpInfo() {
    }

    public HttpInfo(BoundTransportAddress address, long maxContentLength) {
        this.address = address;
        this.maxContentLength = maxContentLength;
    }

    static final class Fields {
        static final XContentBuilderString HTTP = new XContentBuilderString("http");
        static final XContentBuilderString BOUND_ADDRESS = new XContentBuilderString("bound_address");
        static final XContentBuilderString PUBLISH_ADDRESS = new XContentBuilderString("publish_address");
        static final XContentBuilderString MAX_CONTENT_LENGTH = new XContentBuilderString("max_content_length");
        static final XContentBuilderString MAX_CONTENT_LENGTH_IN_BYTES = new XContentBuilderString("max_content_length_in_bytes");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(Fields.HTTP);
        builder.field(Fields.BOUND_ADDRESS, address.boundAddress().toString());
        builder.field(Fields.PUBLISH_ADDRESS, address.publishAddress().toString());
        builder.byteSizeField(Fields.MAX_CONTENT_LENGTH_IN_BYTES, Fields.MAX_CONTENT_LENGTH, maxContentLength);
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
        maxContentLength = in.readLong();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        address.writeTo(out);
        out.writeLong(maxContentLength);
    }

    public BoundTransportAddress address() {
        return address;
    }

    public BoundTransportAddress getAddress() {
        return address();
    }

    public ByteSizeValue maxContentLength() {
        return new ByteSizeValue(maxContentLength);
    }

    public ByteSizeValue getMaxContentLength() {
        return maxContentLength();
    }
}
