
package org.xbib.elasticsearch.websocket;

import org.jboss.netty.channel.Channel;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;

/**
 * InteractiveChannel writes responses to a a channel
 * 
 */
public interface InteractiveChannel {
    
    Channel getChannel();
    
    void sendResponse(InteractiveResponse response) throws IOException;
    
    void sendResponse(String type, Throwable t) throws IOException;
    
    void sendResponse(String type, XContentBuilder builder) throws IOException;
}
