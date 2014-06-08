package org.xbib.elasticsearch.common.netty;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.metrics.CounterMetric;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import java.util.Map;

/**
 *
 */
@ChannelHandler.Sharable
public class OpenChannelsHandler extends SimpleChannelUpstreamHandler {

    final Map<Integer, Channel> openChannels = ConcurrentCollections.newConcurrentMap();

    final CounterMetric openChannelsMetric = new CounterMetric();

    final CounterMetric totalChannelsMetric = new CounterMetric();

    final ESLogger logger;

    public OpenChannelsHandler(ESLogger logger) {
        this.logger = logger;
    }

    final ChannelFutureListener remover = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            Channel channel = openChannels.remove(future.getChannel().getId());
            if (channel != null) {
                openChannelsMetric.dec();
            }
            if (logger.isTraceEnabled()) {
                logger.trace("channel closed: {}", future.getChannel());
            }
        }
    };

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof ChannelStateEvent) {
            ChannelStateEvent evt = (ChannelStateEvent) e;
            // OPEN is also sent to when closing channel, but with FALSE on it to indicate it closes
            if (evt.getState() == ChannelState.OPEN && Boolean.TRUE.equals(evt.getValue())) {
                if (logger.isTraceEnabled()) {
                    logger.trace("channel opened: {}", ctx.getChannel());
                }
                Channel channel = openChannels.put(ctx.getChannel().getId(), ctx.getChannel());
                if (channel == null) {
                    openChannelsMetric.inc();
                    totalChannelsMetric.inc();
                    ctx.getChannel().getCloseFuture().addListener(remover);
                }
            }
        }
        ctx.sendUpstream(e);
    }

    public Channel channel(Integer id) {
        return openChannels.get(id);
    }

    public long numberOfOpenChannels() {
        return openChannelsMetric.count();
    }

    public long totalChannels() {
        return totalChannelsMetric.count();
    }

    public void close() {
        for (Channel channel : openChannels.values()) {
            channel.close().awaitUninterruptibly();
        }
    }
}
