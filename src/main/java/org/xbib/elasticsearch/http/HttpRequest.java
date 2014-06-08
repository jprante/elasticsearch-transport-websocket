package org.xbib.elasticsearch.http;

import org.elasticsearch.rest.RestRequest;

public abstract class HttpRequest extends RestRequest {

    public abstract String getMethod();
}
