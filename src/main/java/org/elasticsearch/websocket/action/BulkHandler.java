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
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.elasticsearch.ElasticSearchIllegalStateException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalClient;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.websocket.BaseInteractiveHandler;
import org.elasticsearch.websocket.InteractiveChannel;
import org.elasticsearch.websocket.InteractiveRequest;

/**
 * The bulk handler is derived from the BulkProcessor, but
 * offers explicit flushing and can receive requests from multiple threads
 * into a global action queue.
 * It supports websocket bulk actions and can write back response to an
 * interactive channel.
 * The bulk volume is not controlled.
 * The default concurrency is 32, the number of actions in a bulk is 100.
 * 
 * @author Jörg Prante <joergprante@gmail.com>
 */
public class BulkHandler extends BaseInteractiveHandler {

    private final BulkHandler.Listener listener;
    private final int concurrentRequests;
    private final int bulkActions;
    private final TimeValue flushInterval;
    private final Semaphore semaphore;
    private final ScheduledThreadPoolExecutor scheduler;
    private final ScheduledFuture scheduledFuture;
    private final AtomicLong executionIdGen = new AtomicLong();
    private final static Queue<ActionRequest> bulk = ConcurrentCollections.newQueue();
    private volatile boolean closed = false;

    @Override
    public void handleRequest(final InteractiveRequest request, final InteractiveChannel channel) {
        // will be overriden by bulk action
    }

    /**
     * A listener for the execution.
     */
    public static interface Listener {

        /**
         * Callback before the bulk is executed.
         */
        void beforeBulk(long executionId, BulkRequest request);

        /**
         * Callback after a successful execution of bulk request.
         */
        void afterBulk(long executionId, BulkRequest request, BulkResponse response);

        /**
         * Callback after a failed execution of bulk request.
         */
        void afterBulk(long executionId, BulkRequest request, Throwable failure);
    }

    /**
     * A listener adapter
     */
    class ListenerAdapter implements BulkHandler.Listener {

        @Override
        public void beforeBulk(long executionId, BulkRequest request) {
        }

        @Override
        public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
        }

        @Override
        public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
        }
    }    
    
    /**
     * A builder used to create a build an instance of a bulk processor.
     */
    public static class Builder {

        private Settings settings;
        private Client client;
        private BulkHandler.Listener listener;
        private int concurrentRequests = 1;
        private int bulkActions = 100;
        private TimeValue flushInterval = null;

        /**
         * Creates a builder of bulk processor with the client to use and the
         * listener that will be used to be notified on the completion of bulk
         * requests.
         */
        public Builder(Client client, BulkHandler.Listener listener) {
            this.client = client;
            this.listener = listener;
        }

        /**
         * Sets the number of concurrent requests allowed to be executed. A
         * value of 0 means that only a single request will be allowed to be
         * executed. A value of 1 means 1 concurrent request is allowed to be
         * executed while accumulating new bulk requests. Defaults to
         * <tt>1</tt>.
         */
        public BulkHandler.Builder setConcurrentRequests(int concurrentRequests) {
            this.concurrentRequests = concurrentRequests;
            return this;
        }

        /**
         * Sets when to flush a new bulk request based on the number of actions
         * currently added. Defaults to <tt>1000</tt>. Can be set to <tt>-1</tt>
         * to disable it.
         */
        public BulkHandler.Builder setBulkActions(int bulkActions) {
            this.bulkActions = bulkActions;
            return this;
        }

        /**
         * Sets a flush interval flushing *any* bulk actions pending if the
         * interval passes. Defaults to not set.
         * <p/>
         * Note, both {@link #setBulkActions(int)} and
         * {@link #setBulkSize(org.elasticsearch.common.unit.ByteSizeValue)} can
         * be set to <tt>-1</tt> with the flush interval set allowing for
         * complete async processing of bulk actions.
         */
        public BulkHandler.Builder setFlushInterval(TimeValue flushInterval) {
            this.flushInterval = flushInterval;
            return this;
        }

        /**
         * Builds a new bulk processor.
         */
        public BulkHandler build() {
            return new BulkHandler(settings, client, listener, concurrentRequests, bulkActions, flushInterval);
        }
    }

    public static BulkHandler.Builder builder(Client client, BulkHandler.Listener listener) {
        return new BulkHandler.Builder(client, listener);
    }

    BulkHandler(Settings settings, Client client) {
        super(settings, client);
        this.listener = new ListenerAdapter();
        this.concurrentRequests = 32;
        this.bulkActions = 100;
        this.semaphore = new Semaphore(concurrentRequests);
        this.flushInterval = null;
        this.scheduler = null;
        this.scheduledFuture = null;
    }

    BulkHandler(Settings settings, Client client, BulkHandler.Listener listener, int concurrentRequests, int bulkActions, @Nullable TimeValue flushInterval) {
        super(settings, client);
        this.listener = listener;
        this.concurrentRequests = concurrentRequests;
        this.bulkActions = bulkActions;
        this.semaphore = new Semaphore(concurrentRequests);
        this.flushInterval = flushInterval;
        if (flushInterval != null) {
            this.scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1, EsExecutors.daemonThreadFactory(((InternalClient) client).settings(), "websocket_bulk_processor"));
            this.scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            this.scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
            this.scheduledFuture = this.scheduler.scheduleWithFixedDelay(new BulkHandler.Flush(), flushInterval.millis(), flushInterval.millis(), TimeUnit.MILLISECONDS);
        } else {
            this.scheduler = null;
            this.scheduledFuture = null;
        }
    }

    public BulkHandler.Listener getListener() {
        return listener;
    }

    /**
     * Flushes open bulk actions
     */
    public synchronized void flush() {
        if (closed) {
            return;
        }
        if (bulk.size() > 0) {
            execute(null);
        }
    }

    /**
     * Closes the processor. If flushing by time is enabled, then its shutdown.
     * Any remaining bulk actions are flushed.
     */
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(false);
            this.scheduler.shutdown();
        }
        if (bulk.size() > 0) {
            execute(null);
        }
    }

    /**
     * Adds an {@link IndexRequest} to the list of actions to execute. Follows
     * the same behavior of {@link IndexRequest} (for example, if no id is
     * provided, one will be generated, or usage of the create flag).
     */
    public BulkHandler add(IndexRequest request) {
        return add((ActionRequest) request);
    }

    /**
     * Adds an {@link DeleteRequest} to the list of actions to execute.
     */
    public BulkHandler add(DeleteRequest request) {
        return add((ActionRequest) request);
    }

    public BulkHandler add(ActionRequest request) {
        internalAdd(request);
        return this;
    }

    private synchronized void internalAdd(ActionRequest request) {
        bulk.add(request);
        executeIfNeeded();
    }

    private void executeIfNeeded() {
        if (closed) {
            throw new ElasticSearchIllegalStateException("bulk process already closed");
        }
        if (!isOverTheLimit()) {
            return;
        }
        execute(null);
    }

    /**
     * Adds an {@link IndexRequest} to the list of actions to execute. Follows
     * the same behavior of {@link IndexRequest} (for example, if no id is
     * provided, one will be generated, or usage of the create flag).
     */
    public BulkHandler add(IndexRequest request, InteractiveChannel channel) {
        return add((ActionRequest) request, channel);
    }

    /**
     * Adds an {@link DeleteRequest} to the list of actions to execute.
     */
    public BulkHandler add(DeleteRequest request, InteractiveChannel channel) {
        return add((ActionRequest) request, channel);
    }

    public BulkHandler add(ActionRequest request, InteractiveChannel channel) {
        internalAdd(request, channel);
        return this;
    }

    private synchronized void internalAdd(ActionRequest request, InteractiveChannel channel) {
        bulk.add(request);
        executeIfNeeded(channel);
    }

    private void executeIfNeeded(InteractiveChannel channel) {
        if (closed) {
            throw new ElasticSearchIllegalStateException("bulk process already closed");
        }
        if (!isOverTheLimit()) {
            return;
        }
        execute(channel);
    }

    // (currently) needs to be executed under a lock
    private void execute(final InteractiveChannel channel) {
        final BulkRequest bulkRequest = new BulkRequest().add(bulk);
        bulk.clear();

        final long executionId = executionIdGen.incrementAndGet();

        if (concurrentRequests == 0) {
            // execute in a blocking fashion...
            try {
                listener.beforeBulk(executionId, bulkRequest);
                listener.afterBulk(executionId, bulkRequest, client.bulk(bulkRequest).actionGet());
            } catch (Exception e) {
                listener.afterBulk(executionId, bulkRequest, e);
            }
        } else {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                listener.afterBulk(executionId, bulkRequest, e);
                return;
            }
            listener.beforeBulk(executionId, bulkRequest);
            client.bulk(bulkRequest, new ActionListener<BulkResponse>() {
                @Override
                public void onResponse(BulkResponse response) {
                    try {
                        listener.afterBulk(executionId, bulkRequest, response);
                        if (channel != null) {
                            channel.sendResponse("bulkresponse", buildResponse(response));
                        }
                    } catch (IOException e) {
                        logger.error("error while sending bulk response", e);
                    } finally {
                        semaphore.release();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        listener.afterBulk(executionId, bulkRequest, t);
                        if (channel != null) {
                            channel.sendResponse("bulkresponse", t);
                        }
                    } catch (IOException e) {
                        logger.error("error while sending bulk response", e);
                    } finally {
                        semaphore.release();
                    }
                }
            });
        }
    }

    private boolean isOverTheLimit() {
        if (bulkActions != -1 && bulk.size() > bulkActions) {
            return true;
        }
        return false;
    }

    class Flush implements Runnable {

        @Override
        public void run() {
            synchronized (BulkHandler.this) {
                if (closed) {
                    return;
                }
                if (bulk.size() > 0) {
                    execute(null);
                }
            }
        }
    }

    /**
     * Taken from the REST bulk action.
     * @param response the bulk response
     * @return a content builder with the response
     * @throws IOException 
     */
    private XContentBuilder buildResponse(BulkResponse response) throws IOException {
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.field(Fields.TOOK, response.tookInMillis());
        builder.startArray(Fields.ITEMS);
        for (BulkItemResponse itemResponse : response) {
            builder.startObject();
            builder.startObject(itemResponse.opType());
            builder.field(Fields._INDEX, itemResponse.index());
            builder.field(Fields._TYPE, itemResponse.type());
            builder.field(Fields._ID, itemResponse.id());
            long version = itemResponse.version();
            if (version != -1) {
                builder.field(Fields._VERSION, itemResponse.version());
            }
            if (itemResponse.failed()) {
                builder.field(Fields.ERROR, itemResponse.failure().message());
            } else {
                builder.field(Fields.OK, true);
            }
            if (itemResponse.response() instanceof IndexResponse) {
                IndexResponse indexResponse = itemResponse.response();
                if (indexResponse.matches() != null) {
                    builder.startArray(Fields.MATCHES);
                    for (String match : indexResponse.matches()) {
                        builder.value(match);
                    }
                    builder.endArray();
                }
            }
            builder.endObject();
            builder.endObject();
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }

    static final class Fields {

        static final XContentBuilderString ITEMS = new XContentBuilderString("items");
        static final XContentBuilderString _INDEX = new XContentBuilderString("_index");
        static final XContentBuilderString _TYPE = new XContentBuilderString("_type");
        static final XContentBuilderString _ID = new XContentBuilderString("_id");
        static final XContentBuilderString ERROR = new XContentBuilderString("error");
        static final XContentBuilderString OK = new XContentBuilderString("ok");
        static final XContentBuilderString TOOK = new XContentBuilderString("took");
        static final XContentBuilderString _VERSION = new XContentBuilderString("_version");
        static final XContentBuilderString MATCHES = new XContentBuilderString("matches");
    }
}
