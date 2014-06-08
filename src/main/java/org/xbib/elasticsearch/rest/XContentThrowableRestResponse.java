package org.xbib.elasticsearch.rest;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;

import static org.elasticsearch.ExceptionsHelper.detailedMessage;
import static org.xbib.elasticsearch.rest.RestXContentBuilder.restContentBuilder;

public class XContentThrowableRestResponse extends XContentRestResponse {

    public XContentThrowableRestResponse(RestRequest request, Throwable t) throws IOException {
        this(request, ((t instanceof ElasticsearchException) ? ((ElasticsearchException) t).status() : RestStatus.INTERNAL_SERVER_ERROR), t);
    }

    public XContentThrowableRestResponse(RestRequest request, RestStatus status, Throwable t) throws IOException {
        super(request, status, convert(request, status, t));
    }

    private static XContentBuilder convert(RestRequest request, RestStatus status, Throwable t) throws IOException {
        XContentBuilder builder = restContentBuilder(request).startObject()
                .field("error", detailedMessage(t))
                .field("status", status.getStatus());
        if (t != null && request.paramAsBoolean("error_trace", false)) {
            builder.startObject("error_trace");
            boolean first = true;
            while (t != null) {
                if (!first) {
                    builder.startObject("cause");
                }
                buildThrowable(t, builder);
                if (!first) {
                    builder.endObject();
                }
                t = t.getCause();
                first = false;
            }
            builder.endObject();
        }
        builder.endObject();
        return builder;
    }

    private static void buildThrowable(Throwable t, XContentBuilder builder) throws IOException {
        builder.field("message", t.getMessage());
        for (StackTraceElement stElement : t.getStackTrace()) {
            builder.startObject("at")
                    .field("class", stElement.getClassName())
                    .field("method", stElement.getMethodName());
            if (stElement.getFileName() != null) {
                builder.field("file", stElement.getFileName());
            }
            if (stElement.getLineNumber() >= 0) {
                builder.field("line", stElement.getLineNumber());
            }
            builder.endObject();
        }
    }
}