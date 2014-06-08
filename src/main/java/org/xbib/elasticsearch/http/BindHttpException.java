package org.xbib.elasticsearch.http;

public class BindHttpException extends HttpException {

    public BindHttpException(String message) {
        super(message);
    }

    public BindHttpException(String message, Throwable cause) {
        super(message, cause);
    }
}