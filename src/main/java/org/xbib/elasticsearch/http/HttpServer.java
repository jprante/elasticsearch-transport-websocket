package org.xbib.elasticsearch.http;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.service.NodeService;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestFilter;
import org.elasticsearch.rest.RestFilterChain;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.xbib.elasticsearch.rest.HttpPatchRestController;
import org.xbib.elasticsearch.websocket.InteractiveController;
import org.xbib.elasticsearch.websocket.Presence;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.elasticsearch.rest.RestStatus.FORBIDDEN;
import static org.elasticsearch.rest.RestStatus.INTERNAL_SERVER_ERROR;
import static org.elasticsearch.rest.RestStatus.NOT_FOUND;
import static org.elasticsearch.rest.RestStatus.OK;

public class HttpServer extends AbstractLifecycleComponent<HttpServer> {

    private final Environment environment;

    private final HttpServerTransport transport;

    private final RestController restController;

    private final HttpPatchRestController httpPatchRestController;

    private final InteractiveController interActiveController;

    private final NodeService nodeService;

    private final boolean disableSites;

    private final PluginSiteFilter pluginSiteFilter = new PluginSiteFilter();

    @Inject
    public HttpServer(Settings settings, Environment environment,
                      HttpServerTransport transport,
                      RestController restController,
                      HttpPatchRestController httpPatchRestController,
                      InteractiveController interActiveController,
                      NodeService nodeService) {
        super(settings);
        this.environment = environment;
        this.transport = transport;
        this.restController = restController;
        this.httpPatchRestController = httpPatchRestController;
        this.interActiveController = interActiveController;
        this.nodeService = nodeService;

        this.disableSites = componentSettings.getAsBoolean("disable_sites", false);

        transport.httpServerAdapter(new Dispatcher(this));

        transport.webSocketServerAdapter(new Interactor(this));

    }

    static class Dispatcher implements HttpServerAdapter {

        private final HttpServer server;

        Dispatcher(HttpServer server) {
            this.server = server;
        }

        @Override
        public void dispatchRequest(HttpRequest request, HttpChannel channel) {
            server.internalDispatchRequest(request, channel);
        }
    }

    static class Interactor implements WebSocketServerAdapter {

        private final HttpServer server;

        Interactor(HttpServer server) {
            this.server = server;
        }

        @Override
        public void presence(Presence presence, String topic, Channel channel) {
            server.internalPresence(presence, topic, channel);
        }

        @Override
        public void frame(WebSocketServerHandshaker handshaker, WebSocketFrame frame, ChannelHandlerContext context) {
            server.internalFrame(handshaker, frame, context);
        }
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        transport.start();
        logger.info("{}", transport.boundAddress());
        nodeService.putAttribute("websocket_address", transport.boundAddress().publishAddress().toString());
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        nodeService.removeAttribute("websocket_address");
        transport.stop();
    }

    @Override
    protected void doClose() throws ElasticsearchException {
        transport.close();
    }

    public TransportAddress address() {
        return transport.boundAddress().publishAddress();
    }

    public HttpInfo info() {
        return transport.info();
    }

    public HttpStats stats() {
        return transport.stats();
    }

    public Channel channel(Integer id) {
        return transport.channel(id);
    }

    public void internalDispatchRequest(final HttpRequest request, final HttpChannel channel) {
        if (request.rawPath().startsWith("/_plugin/")) {
            RestFilterChain filterChain = restController.filterChain(pluginSiteFilter);
            filterChain.continueProcessing(request, channel);
            return;
        }
        try {
            restController.dispatchRequest(request, channel);
        } catch (ElasticsearchIllegalArgumentException e) {
            // unsupported HTTP method, try HTTP PATCH
            httpPatchRestController.dispatchRequest(request, channel);
        }
    }

    public void internalPresence(Presence presence, String topic, Channel channel) {
        interActiveController.presence(presence, topic, channel);
    }

    public void internalFrame(WebSocketServerHandshaker handshaker, WebSocketFrame frame, ChannelHandlerContext context) {
        interActiveController.frame(handshaker, frame, context);
    }


    class PluginSiteFilter extends RestFilter {

        @Override
        public void process(RestRequest request, RestChannel channel, RestFilterChain filterChain) {
            handlePluginSite((HttpRequest) request, (HttpChannel) channel);
        }
    }

    void handlePluginSite(HttpRequest request, HttpChannel channel) {
        if (disableSites) {
            channel.sendResponse(new BytesRestResponse(FORBIDDEN));
            return;
        }
        if (request.method().name().equals("OPTIONS")) {
            // when we have OPTIONS request, simply send OK by default (with the Access Control Origin header which gets automatically added)
            channel.sendResponse(new BytesRestResponse(OK));
            return;
        }
        if (request.method().name().equals("GET")) {
            channel.sendResponse(new BytesRestResponse(FORBIDDEN));
            return;
        }
        // TODO for a "/_plugin" endpoint, we should have a page that lists all the plugins?

        String path = request.rawPath().substring("/_plugin/".length());
        int i1 = path.indexOf('/');
        String pluginName;
        String sitePath;
        if (i1 == -1) {
            pluginName = path;
            sitePath = null;
            // If a trailing / is missing, we redirect to the right page #2654
            channel.sendResponse(new BytesRestResponse(RestStatus.MOVED_PERMANENTLY, "text/html", "<head><meta http-equiv=\"refresh\" content=\"0; URL=" + request.rawPath() + "/\"></head>"));
            return;
        } else {
            pluginName = path.substring(0, i1);
            sitePath = path.substring(i1 + 1);
        }

        if (sitePath.length() == 0) {
            sitePath = "/index.html";
        }

        // Convert file separators.
        sitePath = sitePath.replace('/', File.separatorChar);

        // this is a plugin provided site, serve it as static files from the plugin location
        File siteFile = new File(new File(environment.pluginsFile(), pluginName), "_site");
        File file = new File(siteFile, sitePath);
        if (!file.exists() || file.isHidden()) {
            channel.sendResponse(new BytesRestResponse(NOT_FOUND));
            return;
        }
        if (!file.isFile()) {
            // If it's not a dir, we send a 403
            if (!file.isDirectory()) {
                channel.sendResponse(new BytesRestResponse(FORBIDDEN));
                return;
            }
            // We don't serve dir but if index.html exists in dir we should serve it
            file = new File(file, "index.html");
            if (!file.exists() || file.isHidden() || !file.isFile()) {
                channel.sendResponse(new BytesRestResponse(FORBIDDEN));
                return;
            }
        }
        if (!file.getAbsolutePath().startsWith(siteFile.getAbsolutePath())) {
            channel.sendResponse(new BytesRestResponse(FORBIDDEN));
            return;
        }
        try {
            byte[] data = Streams.copyToByteArray(file);
            channel.sendResponse(new BytesRestResponse(OK, guessMimeType(sitePath), data));
        } catch (IOException e) {
            channel.sendResponse(new BytesRestResponse(INTERNAL_SERVER_ERROR));
        }
    }


    // TODO: Don't respond with a mime type that violates the request's Accept header
    private String guessMimeType(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        String extension = path.substring(lastDot + 1).toLowerCase(Locale.ROOT);
        String mimeType = DEFAULT_MIME_TYPES.get(extension);
        if (mimeType == null) {
            return "";
        }
        return mimeType;
    }

    static {
        // This is not an exhaustive list, just the most common types. Call registerMimeType() to add more.
        Map<String, String> mimeTypes = new HashMap<>();
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("csv", "text/csv");
        mimeTypes.put("htm", "text/html");
        mimeTypes.put("html", "text/html");
        mimeTypes.put("xml", "text/xml");
        mimeTypes.put("js", "text/javascript"); // Technically it should be application/javascript (RFC 4329), but IE8 struggles with that
        mimeTypes.put("xhtml", "application/xhtml+xml");
        mimeTypes.put("json", "application/json");
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("zip", "application/zip");
        mimeTypes.put("tar", "application/x-tar");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("tiff", "image/tiff");
        mimeTypes.put("tif", "image/tiff");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("svg", "image/svg+xml");
        mimeTypes.put("ico", "image/vnd.microsoft.icon");
        mimeTypes.put("mp3", "audio/mpeg");
        DEFAULT_MIME_TYPES = ImmutableMap.copyOf(mimeTypes);
    }

    public static final Map<String, String> DEFAULT_MIME_TYPES;

}
