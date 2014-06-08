package org.xbib.elasticsearch.http.netty.client;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpHeaders.Values;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.websocketx.WebSocket00FrameDecoder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.util.CharsetUtil;
import org.xbib.elasticsearch.websocket.client.WebSocketActionListener;
import org.xbib.elasticsearch.websocket.client.WebSocketClient;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Handles socket communication for a connected WebSocket client. Not intended
 * for end-users. Please use {@link NettyWebSocketClient} for controlling your client.
 */
public class NettyWebSocketClientHandler extends SimpleChannelUpstreamHandler
        implements WebSocketClient {

    private final ClientBootstrap bootstrap;
    private final URI url;
    private final WebSocketActionListener listener;
    private boolean handshakeCompleted = false;
    private Channel channel;

    public NettyWebSocketClientHandler(ClientBootstrap bootstrap, URI url, WebSocketActionListener listener) {
        this.bootstrap = bootstrap;
        this.url = url;
        this.listener = listener;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
        String path = url.getPath();
        if (url.getQuery() != null && url.getQuery().length() > 0) {
            path = url.getPath() + "?" + url.getQuery();
        }
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
        request.headers().add(Names.UPGRADE, Values.WEBSOCKET);
        request.headers().add(Names.CONNECTION, Values.UPGRADE);
        request.headers().add(Names.HOST, url.getHost());
        request.headers().add(Names.ORIGIN, "http://" + url.getHost());
        event.getChannel().write(request);
        ctx.getPipeline().replace("encoder", "ws-encoder", new WebSocket00FrameEncoder());
        this.channel = event.getChannel();
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
        listener.onDisconnect(this);
        handshakeCompleted = false;
        channel = null;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
        if (!handshakeCompleted) {
            HttpResponse response = (HttpResponse) event.getMessage();
            final HttpResponseStatus status = new HttpResponseStatus(101, "Web Socket Protocol Handshake");
            final boolean validStatus = response.getStatus().equals(status);
            final boolean validUpgrade = response.headers().get(Names.UPGRADE).equals(Values.WEBSOCKET);
            final boolean validConnection = response.headers().get(Names.CONNECTION).equals(Values.UPGRADE);
            if (!validStatus || !validUpgrade || !validConnection) {
                throw new NettyWebSocketException("Invalid handshake response");
            }
            handshakeCompleted = true;
            ctx.getPipeline().replace("decoder", "ws-decoder", new WebSocket00FrameDecoder());
            listener.onConnect(this);
            return;
        }
        if (event.getMessage() instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) event.getMessage();
            throw new NettyWebSocketException("Unexpected HttpResponse (status=" + response.getStatus() + ", content=" + response.getContent().toString(CharsetUtil.UTF_8) + ")");
        }
        WebSocketFrame frame = (WebSocketFrame) event.getMessage();
        listener.onMessage(this, frame);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        final Throwable t = e.getCause();
        listener.onError(t);
        e.getChannel().close();
    }

    @Override
    public ChannelFuture connect() {
        return bootstrap.connect(new InetSocketAddress(url.getHost(), url.getPort()));
    }

    @Override
    public ChannelFuture disconnect() {
        if (channel == null) {
            return null;
        }
        return channel.close();
    }

    @Override
    public ChannelFuture send(WebSocketFrame frame) {
        if (channel == null) {
            return null;
        }
        return channel.write(frame);
    }

}