
package org.xbib.elasticsearch.websocket.http;

import org.elasticsearch.ElasticsearchException;

public class HttpException extends ElasticsearchException {

    public HttpException(String message) {
        super(message);
    }

    public HttpException(String message, Throwable cause) {
        super(message, cause);
    }
}