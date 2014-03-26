
package org.xbib.elasticsearch.websocket.http.netty;

import org.jboss.netty.channel.Channel;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.xbib.elasticsearch.websocket.InteractiveChannel;
import org.xbib.elasticsearch.websocket.InteractiveResponse;

/**
 * Netty implementation for an interactive channel
 */
public class NettyInteractiveChannel implements InteractiveChannel {

    private final Channel channel;
    
    public NettyInteractiveChannel(Channel channel) {
        this.channel = channel;
    }
    
    @Override
    public Channel getChannel() {
        return channel;
    }
    
    @Override
    public void sendResponse(InteractiveResponse response) throws IOException{
        channel.write(response.response());
    }
    
    @Override
    public void sendResponse(String type, Throwable t) throws IOException{
        channel.write(new NettyInteractiveResponse(type, t).response());
    }
    
    @Override
    public void sendResponse(String type, XContentBuilder builder) throws IOException{
        channel.write(new NettyInteractiveResponse(type, builder).response());
    }
}
