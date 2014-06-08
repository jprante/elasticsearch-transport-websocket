package org.xbib.elasticsearch.http;

import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestRequest;

public abstract class HttpChannel extends RestChannel {

    protected HttpChannel(RestRequest request) {
        super(request);
    }
}
