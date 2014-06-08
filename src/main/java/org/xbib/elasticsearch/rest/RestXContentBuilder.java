package org.xbib.elasticsearch.rest;

import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestRequest;

import java.io.IOException;

public class RestXContentBuilder {

    public static XContentBuilder restContentBuilder(RestRequest request) throws IOException {
        XContentType contentType = XContentType.fromRestContentType(request.param("format", request.header("Content-Type")));
        if (contentType == null) {
            // default to JSON
            contentType = XContentType.JSON;
        }
        XContentBuilder builder = new XContentBuilder(XContentFactory.xContent(contentType),
                new BytesStreamOutput());
        if (request.paramAsBoolean("pretty", false)) {
            builder.prettyPrint().lfAtEnd();
        }
        String casing = request.param("case");
        if (casing != null && "camelCase".equals(casing)) {
            builder.fieldCaseConversion(XContentBuilder.FieldCaseConversion.CAMELCASE);
        } else {
            // we expect all REST interfaces to write results in underscore casing, so
            // no need for double casing
            builder.fieldCaseConversion(XContentBuilder.FieldCaseConversion.NONE);
        }
        return builder;
    }

    public static XContentBuilder emptyBuilder(RestRequest request) throws IOException {
        return restContentBuilder(request).startObject().endObject();
    }

}
