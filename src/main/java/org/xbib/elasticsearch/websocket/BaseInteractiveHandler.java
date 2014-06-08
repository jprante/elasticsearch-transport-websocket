package org.xbib.elasticsearch.websocket;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;

/**
 * The BaseInteractiveHandler is the base class for interactive actions.
 */
public abstract class BaseInteractiveHandler extends AbstractComponent
        implements InteractiveHandler {

    protected final Client client;

    protected BaseInteractiveHandler(Settings settings, Client client) {
        super(settings);
        this.client = client;
    }
}
