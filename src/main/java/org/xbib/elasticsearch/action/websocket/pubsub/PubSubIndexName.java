package org.xbib.elasticsearch.action.websocket.pubsub;

import org.elasticsearch.common.inject.BindingAnnotation;
import org.elasticsearch.common.settings.Settings;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Configuration for the pubsub index name.
 */
@BindingAnnotation
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Documented
public @interface PubSubIndexName {

    static class Conf {
        public static final String DEFAULT_INDEX_NAME = "pubsub";

        public static String indexName(Settings settings) {
            return settings.get("pubsub.index_name", DEFAULT_INDEX_NAME);
        }
    }
}
