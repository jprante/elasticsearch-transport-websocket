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
package org.elasticsearch.websocket.action;

import java.io.IOException;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.websocket.InteractiveChannel;

/*
 * Checkpoint management for topics and subscribers.
 * 
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class Checkpointer extends AbstractLifecycleComponent<Checkpointer> {

    private final String pubSubIndexName;
    private final Client client;
    private final static String TYPE = "checkpoint";
    private final BulkHandler bulkHandler;

    @Inject
    public Checkpointer(Settings settings, Client client) {
        super(settings);
        this.client = client;
        this.bulkHandler = new BulkHandler(settings, client);
        this.pubSubIndexName = PubSubIndexName.Conf.indexName(settings);
    }

    @Override
    protected void doStart() throws ElasticSearchException {
    }

    @Override
    protected void doStop() throws ElasticSearchException {
    }

    @Override
    protected void doClose() throws ElasticSearchException {
    }

    /**
     * Checkpointing a topic or a subscriber. The current timestamp is written
     * to the checkpoint index type. Note that bulk index requests are used by
     * checkpointing and flushCheckpoint() needs to be called after all is done.
     *
     * @param id topic or subscriber
     * @throws IOException
     */
    public void checkpoint(String id) throws IOException {
        indexBulk(Requests.indexRequest(pubSubIndexName).type(TYPE).id(id)
                .source(jsonBuilder().startObject().field("timestamp", System.currentTimeMillis()).endObject()), null);
    }

    public void flushCheckpoint() throws IOException {
        flushBulk(null);
    }

    public Long checkpointedAt(String id) throws IOException {
        GetResponse response = client.prepareGet(pubSubIndexName, TYPE, id)
                .setFields("timestamp")
                .execute().actionGet();
        boolean failed = !response.exists();
        if (failed) {
            logger.warn("can't get checkpoint for {}", id);
            return null;
        } else {
            return (Long) response.getFields().get("timestamp").getValue();
        }
    }

    /**
     * Perform bulk indexing
     *
     * @param request the index request
     * @param channel the interactive channel
     * @throws IOException
     */
    public void indexBulk(IndexRequest request, InteractiveChannel channel) throws IOException {
        bulkHandler.add(request, channel);
    }

    /**
     * Perform bulk delete
     *
     * @param request the delete request
     * @param channel the interactive channel
     * @throws IOException
     */
    public void deleteBulk(DeleteRequest request, InteractiveChannel channel) throws IOException {
        bulkHandler.add(request, channel);
    }

    /**
     * Flush bulk
     *
     * @param channel the interactive channel
     * @throws IOException
     */
    public void flushBulk(InteractiveChannel channel) throws IOException {
        bulkHandler.flush();
    }
}