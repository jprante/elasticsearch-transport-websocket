package org.xbib.elasticsearch.rest;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;

public class XContentRestResponse extends BytesRestResponse {

    public XContentRestResponse(RestRequest request, RestStatus status, XContentBuilder builder) throws IOException {
        super(status, builder);
    }

}
