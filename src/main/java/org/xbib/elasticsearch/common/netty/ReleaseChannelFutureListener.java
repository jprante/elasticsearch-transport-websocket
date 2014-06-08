package org.xbib.elasticsearch.common.netty;

import org.elasticsearch.common.lease.Releasable;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

public class ReleaseChannelFutureListener implements ChannelFutureListener {

    private final Releasable releasable;

    public ReleaseChannelFutureListener(Releasable releasable) {
        this.releasable = releasable;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        releasable.close();
    }
}
