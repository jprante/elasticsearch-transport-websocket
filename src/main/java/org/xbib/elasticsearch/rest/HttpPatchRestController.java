package org.xbib.elasticsearch.rest;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.path.PathTrie;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.support.RestUtils;
import org.xbib.elasticsearch.http.netty.NettyHttpRequest;

public class HttpPatchRestController extends AbstractLifecycleComponent<HttpPatchRestController> {

    private final PathTrie<RestHandler> patchHandlers = new PathTrie<>(RestUtils.REST_DECODER);

    @Inject
    public HttpPatchRestController(Settings settings) {
        super(settings);
    }

    @Override
    protected void doStart() throws ElasticsearchException {

    }

    @Override
    protected void doStop() throws ElasticsearchException {

    }

    @Override
    protected void doClose() throws ElasticsearchException {

    }

    public void registerHandler(String method, String path, RestHandler handler) {
        if ("PATCH".equalsIgnoreCase(method)) {
            patchHandlers.insert(path, handler);
        }
    }

    public void dispatchRequest(final RestRequest request, final RestChannel channel) {
        try {
            if (request instanceof NettyHttpRequest) {
                NettyHttpRequest nettyHttpRequest = (NettyHttpRequest)request;
                if ("PATCH".equalsIgnoreCase(nettyHttpRequest.getMethod())) {
                    RestHandler handler = patchHandlers.retrieve(request.rawPath());
                    if (handler != null) {
                        handler.handleRequest(request, channel);
                    }
                }
            }
        } catch (Throwable e) {
            try {
                channel.sendResponse(new BytesRestResponse(channel, e));
            } catch (Throwable e1) {
                logger.error("failed to send failure response for uri [" + request.uri() + "]", e1);
            }
        }
    }
}
