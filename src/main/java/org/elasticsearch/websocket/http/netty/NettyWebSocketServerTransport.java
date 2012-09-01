/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.websocket.http.netty;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.network.NetworkService;
import static org.elasticsearch.common.network.NetworkService.TcpSettings.*;
import org.elasticsearch.common.network.NetworkUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.BoundTransportAddress;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.NetworkExceptionHelper;
import org.elasticsearch.common.transport.PortsRange;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import static org.elasticsearch.common.util.concurrent.EsExecutors.daemonThreadFactory;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.monitor.jvm.JvmInfo;
import org.elasticsearch.transport.BindTransportException;
import org.elasticsearch.websocket.Presence;
import org.elasticsearch.websocket.action.Checkpointer;
import org.elasticsearch.websocket.client.WebSocketActionListener;
import org.elasticsearch.websocket.client.WebSocketClient;
import org.elasticsearch.websocket.common.netty.NettyStaticSetup;
import org.elasticsearch.websocket.common.netty.OpenChannelsHandler;
import org.elasticsearch.websocket.http.BindHttpException;
import org.elasticsearch.websocket.http.HttpChannel;
import org.elasticsearch.websocket.http.HttpRequest;
import org.elasticsearch.websocket.http.HttpServerAdapter;
import org.elasticsearch.websocket.http.HttpServerTransport;
import org.elasticsearch.websocket.http.HttpStats;
import org.elasticsearch.websocket.http.WebSocketServerAdapter;
import org.elasticsearch.websocket.http.netty.client.NettyWebSocketClientFactory;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.AdaptiveReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.ReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.timeout.ReadTimeoutException;

/**
 * WebSocket server transport. Based on HttpServerTransport.
 * Extended for channel lookup and message forwarding.
 * 
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class NettyWebSocketServerTransport
        extends AbstractLifecycleComponent<HttpServerTransport>
        implements HttpServerTransport {

    static {
        NettyStaticSetup.setup();
    }
    private final NetworkService networkService;
    private final Checkpointer pubsubService;
    final ByteSizeValue maxContentLength;
    final ByteSizeValue maxInitialLineLength;
    final ByteSizeValue maxHeaderSize;
    final ByteSizeValue maxChunkSize;
    private final int workerCount;
    final boolean compression;
    final int compressionLevel;
    final boolean resetCookies;
    private final String port;
    private final String bindHost;
    private final String publishHost;
    private final Boolean tcpNoDelay;
    private final Boolean tcpKeepAlive;
    private final Boolean reuseAddress;
    private final ByteSizeValue tcpSendBufferSize;
    private final ByteSizeValue tcpReceiveBufferSize;
    private final ReceiveBufferSizePredictorFactory receiveBufferSizePredictorFactory;
    final ByteSizeValue maxCumulationBufferCapacity;
    final int maxCompositeBufferComponents;
    private volatile ServerBootstrap serverBootstrap;
    private volatile BoundTransportAddress boundAddress;
    private volatile Channel serverChannel;
    protected OpenChannelsHandler serverOpenChannels;
    private volatile HttpServerAdapter httpServerAdapter;
    private volatile WebSocketServerAdapter webSocketServerAdapter;
    private Map<String, WebSocketClient> nodeChannels = ConcurrentCollections.newConcurrentMap();
    private final static NettyWebSocketClientFactory clientFactory = new NettyWebSocketClientFactory();

    @Inject
    public NettyWebSocketServerTransport(Settings settings, NetworkService networkService, Checkpointer pubsubService) {
        super(settings);
        this.networkService = networkService;
        this.pubsubService = pubsubService;
        ByteSizeValue maxContentLength = componentSettings.getAsBytesSize("max_content_length", settings.getAsBytesSize("websocket.max_content_length", new ByteSizeValue(100, ByteSizeUnit.MB)));
        this.maxChunkSize = componentSettings.getAsBytesSize("max_chunk_size", settings.getAsBytesSize("websocket.max_chunk_size", new ByteSizeValue(8, ByteSizeUnit.KB)));
        this.maxHeaderSize = componentSettings.getAsBytesSize("max_header_size", settings.getAsBytesSize("websocket.max_header_size", new ByteSizeValue(8, ByteSizeUnit.KB)));
        this.maxInitialLineLength = componentSettings.getAsBytesSize("max_initial_line_length", settings.getAsBytesSize("websocket.max_initial_line_length", new ByteSizeValue(4, ByteSizeUnit.KB)));
        // don't reset cookies by default, since I don't think we really need to
        // note, parsing cookies was fixed in netty 3.5.1 regarding stack allocation, but still, currently, we don't need cookies
        this.resetCookies = componentSettings.getAsBoolean("reset_cookies", settings.getAsBoolean("websocket.reset_cookies", false));
        this.maxCumulationBufferCapacity = componentSettings.getAsBytesSize("max_cumulation_buffer_capacity", null);
        this.maxCompositeBufferComponents = componentSettings.getAsInt("max_composite_buffer_components", -1);
        this.workerCount = componentSettings.getAsInt("worker_count", Runtime.getRuntime().availableProcessors() * 2);
        this.port = componentSettings.get("port", settings.get("websocket.port", "9400-9500"));
        this.bindHost = componentSettings.get("bind_host", settings.get("websocket.bind_host", settings.get("websocket.host")));
        this.publishHost = componentSettings.get("publish_host", settings.get("websocket.publish_host", settings.get("websocket.host")));
        this.tcpNoDelay = componentSettings.getAsBoolean("tcp_no_delay", settings.getAsBoolean(TCP_NO_DELAY, true));
        this.tcpKeepAlive = componentSettings.getAsBoolean("tcp_keep_alive", settings.getAsBoolean(TCP_KEEP_ALIVE, true));
        this.reuseAddress = componentSettings.getAsBoolean("reuse_address", settings.getAsBoolean(TCP_REUSE_ADDRESS, NetworkUtils.defaultReuseAddress()));
        this.tcpSendBufferSize = componentSettings.getAsBytesSize("tcp_send_buffer_size", settings.getAsBytesSize(TCP_SEND_BUFFER_SIZE, TCP_DEFAULT_SEND_BUFFER_SIZE));
        this.tcpReceiveBufferSize = componentSettings.getAsBytesSize("tcp_receive_buffer_size", settings.getAsBytesSize(TCP_RECEIVE_BUFFER_SIZE, TCP_DEFAULT_RECEIVE_BUFFER_SIZE));

        long defaultReceiverPredictor = 512 * 1024;
        if (JvmInfo.jvmInfo().mem().directMemoryMax().bytes() > 0) {
            // we can guess a better default...
            long l = (long) ((0.3 * JvmInfo.jvmInfo().mem().directMemoryMax().bytes()) / workerCount);
            defaultReceiverPredictor = Math.min(defaultReceiverPredictor, Math.max(l, 64 * 1024));
        }

        // See AdaptiveReceiveBufferSizePredictor#DEFAULT_XXX for default values in netty..., we can use higher ones for us, even fixed one
        ByteSizeValue receivePredictorMin = componentSettings.getAsBytesSize("receive_predictor_min", componentSettings.getAsBytesSize("receive_predictor_size", new ByteSizeValue(defaultReceiverPredictor)));
        ByteSizeValue receivePredictorMax = componentSettings.getAsBytesSize("receive_predictor_max", componentSettings.getAsBytesSize("receive_predictor_size", new ByteSizeValue(defaultReceiverPredictor)));
        if (receivePredictorMax.bytes() == receivePredictorMin.bytes()) {
            receiveBufferSizePredictorFactory = new FixedReceiveBufferSizePredictorFactory((int) receivePredictorMax.bytes());
        } else {
            receiveBufferSizePredictorFactory = new AdaptiveReceiveBufferSizePredictorFactory((int) receivePredictorMin.bytes(), (int) receivePredictorMin.bytes(), (int) receivePredictorMax.bytes());
        }

        this.compression = settings.getAsBoolean("websocket.compression", false);
        this.compressionLevel = settings.getAsInt("websocket.compression_level", 6);

        // validate max content length
        if (maxContentLength.bytes() > Integer.MAX_VALUE) {
            logger.warn("maxContentLength[" + maxContentLength + "] set to high value, resetting it to [100mb]");
            maxContentLength = new ByteSizeValue(100, ByteSizeUnit.MB);
        }
        this.maxContentLength = maxContentLength;

        logger.debug("using max_chunk_size[{}], max_header_size[{}], max_initial_line_length[{}], max_content_length[{}], receive_predictor[{}->{}]",
                maxChunkSize, maxHeaderSize, maxInitialLineLength, this.maxContentLength, receivePredictorMin, receivePredictorMax);
    }

    @Override
    public void httpServerAdapter(HttpServerAdapter httpServerAdapter) {
        this.httpServerAdapter = httpServerAdapter;
    }

    @Override
    public void webSocketServerAdapter(WebSocketServerAdapter webSocketServerAdapter) {
        this.webSocketServerAdapter = webSocketServerAdapter;
    }

    @Override
    protected void doStart() throws ElasticSearchException {
        this.serverOpenChannels = new OpenChannelsHandler(logger);

        /* we do not support oio for websocket - it wouldn't work */
        serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(daemonThreadFactory(settings, "websocket_server_boss")),
                Executors.newCachedThreadPool(daemonThreadFactory(settings, "websocket_server_worker")),
                workerCount));

        serverBootstrap.setPipelineFactory(new NettyWebSocketServerPipelineFactory(this));

        if (tcpNoDelay != null) {
            serverBootstrap.setOption("child.tcpNoDelay", tcpNoDelay);
        }
        if (tcpKeepAlive != null) {
            serverBootstrap.setOption("child.keepAlive", tcpKeepAlive);
        }
        if (tcpSendBufferSize != null && tcpSendBufferSize.bytes() > 0) {
            serverBootstrap.setOption("child.sendBufferSize", tcpSendBufferSize.bytes());
        }
        if (tcpReceiveBufferSize != null && tcpReceiveBufferSize.bytes() > 0) {
            serverBootstrap.setOption("child.receiveBufferSize", tcpReceiveBufferSize.bytes());
        }
        serverBootstrap.setOption("receiveBufferSizePredictorFactory", receiveBufferSizePredictorFactory);
        serverBootstrap.setOption("child.receiveBufferSizePredictorFactory", receiveBufferSizePredictorFactory);
        if (reuseAddress != null) {
            serverBootstrap.setOption("reuseAddress", reuseAddress);
            serverBootstrap.setOption("child.reuseAddress", reuseAddress);
        }

        // Bind and start to accept incoming connections.
        InetAddress hostAddressX;
        try {
            hostAddressX = networkService.resolveBindHostAddress(bindHost);
        } catch (IOException e) {
            throw new BindHttpException("Failed to resolve host [" + bindHost + "]", e);
        }
        final InetAddress hostAddress = hostAddressX;

        PortsRange portsRange = new PortsRange(port);
        final AtomicReference<Exception> lastException = new AtomicReference();
        boolean success = portsRange.iterate(new PortsRange.PortCallback() {
            @Override
            public boolean onPortNumber(int portNumber) {
                try {
                    serverChannel = serverBootstrap.bind(new InetSocketAddress(hostAddress, portNumber));
                } catch (Exception e) {
                    lastException.set(e);
                    return false;
                }
                return true;
            }
        });
        if (!success) {
            throw new BindHttpException("Failed to bind to [" + port + "]", lastException.get());
        }
        InetSocketAddress boundAddress = (InetSocketAddress) serverChannel.getLocalAddress();
        InetSocketAddress publishAddress;
        try {
            publishAddress = new InetSocketAddress(networkService.resolvePublishHostAddress(publishHost), boundAddress.getPort());
        } catch (Exception e) {
            throw new BindTransportException("Failed to resolve publish address", e);
        }
        this.boundAddress = new BoundTransportAddress(new InetSocketTransportAddress(boundAddress), new InetSocketTransportAddress(publishAddress));
    }

    @Override
    protected void doStop() throws ElasticSearchException {
        if (serverChannel != null) {
            serverChannel.close().awaitUninterruptibly();
            serverChannel = null;
        }

        if (serverOpenChannels != null) {
            serverOpenChannels.close();
            serverOpenChannels = null;
        }

        if (serverBootstrap != null) {
            serverBootstrap.releaseExternalResources();
            serverBootstrap = null;
        }
    }

    @Override
    protected void doClose() throws ElasticSearchException {
    }

    @Override
    public BoundTransportAddress boundAddress() {
        return this.boundAddress;
    }

    @Override
    public HttpStats stats() {
        OpenChannelsHandler channels = serverOpenChannels;
        return new HttpStats(channels == null ? 0 : channels.numberOfOpenChannels(), channels == null ? 0 : channels.totalChannels());
    }

    /**
     * Dispatch reqeuest to HTTP adapter.
     *
     * @param request
     * @param channel
     */
    void dispatchRequest(HttpRequest request, HttpChannel channel) {
        httpServerAdapter.dispatchRequest(request, channel);
    }

    /**
     * A channel appeared or disappeared.
     *
     * @param presence
     * @param topic
     * @param channel
     */
    void presence(Presence presence, String topic, Channel channel) throws IOException {
        webSocketServerAdapter.presence(presence, topic, channel);
    }

    /**
     * Send a websocket frame.
     *
     * @param handshaker
     * @param frame
     * @param context
     */
    void frame(WebSocketServerHandshaker handshaker, WebSocketFrame frame, ChannelHandlerContext context) {
        webSocketServerAdapter.frame(handshaker, frame, context);
    }

    /**
     * Returns a channel if it is in the server open channel table, given by ID.
     *
     * @param id the channel ID
     * @return the channel if open or null if not present.
     */
    @Override
    public Channel channel(Integer id) {
        return serverOpenChannels.channel(id);
    }

    /**
     * Forward a message to another node with websockets for delivery.
     *
     * @param websocketNodeAddress the websocket address of the other node, e.g.
     * "/10.0.0.1:9400"
     * @param channelId the channel ID on the other node for delivering the
     * message
     * @param builder the builder for the message
     */
    @Override
    public void forward(final String websocketNodeAddress, final Integer channelId, final XContentBuilder builder) {
        try {
            // build "forward" text frame
            XContentBuilder forwardBuilder = jsonBuilder();
            forwardBuilder.startObject()
                    .field("channel", channelId)
                    .rawField("message", builder.bytes())
                    .endObject();
            final TextWebSocketFrame frame = new NettyInteractiveResponse("forward", forwardBuilder).response();
            // use a websocket client pool
            WebSocketClient client = nodeChannels.get(websocketNodeAddress);
            if (client == null) {
                final URI uri = new URI("ws:/" + websocketNodeAddress + "/websocket");
                client = clientFactory.newClient(uri, new WebSocketActionListener() {
                    @Override
                    public void onConnect(WebSocketClient client) {
                        nodeChannels.put(websocketNodeAddress, client);
                        client.send(frame);
                    }

                    @Override
                    public void onDisconnect(WebSocketClient client) {
                        logger.warn("node disconnected: {}", uri);
                        nodeChannels.remove(websocketNodeAddress);
                    }

                    @Override
                    public void onMessage(WebSocketClient client, WebSocketFrame frame) {
                        logger.info("unexpected response {}", frame);
                    }

                    @Override
                    public void onError(Throwable t) {
                        logger.error(t.getMessage(), t);
                        nodeChannels.remove(websocketNodeAddress);
                    }
                });
                client.connect();
            } else {
                client.send(frame);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (e.getCause() instanceof ReadTimeoutException) {
            if (logger.isTraceEnabled()) {
                logger.trace("Connection timeout [{}]", ctx.getChannel().getRemoteAddress());
            }
            ctx.getChannel().close();
        } else {
            if (!lifecycle.started()) {
                // ignore
                return;
            }
            if (!NetworkExceptionHelper.isCloseConnectionException(e.getCause())) {
                logger.warn("Caught exception while handling client http traffic, closing connection {}", e.getCause(), ctx.getChannel());
                ctx.getChannel().close();
            } else {
                logger.debug("Caught exception while handling client http traffic, closing connection {}", e.getCause(), ctx.getChannel());
                ctx.getChannel().close();
            }
        }
    }
    

    
}
