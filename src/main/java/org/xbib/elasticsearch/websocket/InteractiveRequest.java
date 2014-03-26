
package org.xbib.elasticsearch.websocket;

import java.util.Map;
import org.elasticsearch.common.unit.TimeValue;

/**
 * The InteractiveRequest manages parameters in an interaction.
 */
public interface InteractiveRequest {

     Map<String,Object> asMap();
    
     boolean hasParam(String key);

     Object param(String key);

     String paramAsString(String key);
     
     String paramAsString(String key, String defaultValue);

     long paramAsLong(String key);
     
     long paramAsLong(String key, long defaultValue);
     
     boolean paramAsBoolean(String key);
     
     boolean paramAsBoolean(String key, boolean defaultValue);
     
     TimeValue paramAsTime(String key);
     
     TimeValue paramAsTime(String key, TimeValue defaultValue);
     
}
