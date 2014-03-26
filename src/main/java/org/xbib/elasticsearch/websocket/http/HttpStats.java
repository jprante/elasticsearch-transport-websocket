
package org.xbib.elasticsearch.websocket.http;

import java.io.IOException;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;

public class HttpStats implements Streamable, ToXContent {

    private long serverOpen;
    private long totalOpen;

    HttpStats() {
    }

    public HttpStats(long serverOpen, long totalOpen) {
        this.serverOpen = serverOpen;
        this.totalOpen = totalOpen;
    }

    public long serverOpen() {
        return this.serverOpen;
    }

    public long getServerOpen() {
        return serverOpen();
    }

    public long totalOpen() {
        return this.totalOpen;
    }

    public long getTotalOpen() {
        return this.totalOpen;
    }

    public static HttpStats readHttpStats(StreamInput in) throws IOException {
        HttpStats stats = new HttpStats();
        stats.readFrom(in);
        return stats;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        serverOpen = in.readVLong();
        totalOpen = in.readVLong();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVLong(serverOpen);
        out.writeVLong(totalOpen);
    }

    static final class Fields {
        static final XContentBuilderString WEBSOCKET = new XContentBuilderString("websocket");
        static final XContentBuilderString CURRENT_OPEN = new XContentBuilderString("current_open");
        static final XContentBuilderString TOTAL_OPENED = new XContentBuilderString("total_opened");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(Fields.WEBSOCKET);
        builder.field(Fields.CURRENT_OPEN, serverOpen);
        builder.field(Fields.TOTAL_OPENED, totalOpen);
        builder.endObject();
        return builder;
    }
}