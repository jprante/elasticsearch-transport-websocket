package org.xbib.elasticsearch.transport.netty;

import org.elasticsearch.Version;
import org.elasticsearch.common.component.Lifecycle;
import org.elasticsearch.common.io.ThrowableObjectInputStream;
import org.elasticsearch.common.io.stream.CachedStreamInput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.ActionNotFoundTransportException;
import org.elasticsearch.transport.RemoteTransportException;
import org.elasticsearch.transport.ResponseHandlerFailureTransportException;
import org.elasticsearch.transport.TransportRequest;
import org.elasticsearch.transport.TransportRequestHandler;
import org.elasticsearch.transport.TransportResponse;
import org.elasticsearch.transport.TransportResponseHandler;
import org.elasticsearch.transport.TransportSerializationException;
import org.elasticsearch.transport.TransportServiceAdapter;
import org.elasticsearch.transport.support.TransportStatus;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import java.io.IOException;

/**
 * A handler (must be the last one!) that does size based frame decoding and forwards the actual message
 * to the relevant action.
 */
public class MessageChannelHandler extends SimpleChannelUpstreamHandler {

    private final ESLogger logger;

    private final ThreadPool threadPool;

    private final TransportServiceAdapter transportServiceAdapter;

    private final NettyTransport transport;

    public MessageChannelHandler(NettyTransport transport, ESLogger logger) {
        this.threadPool = transport.threadPool();
        this.transportServiceAdapter = transport.transportServiceAdapter();
        this.transport = transport;
        this.logger = logger;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object m = e.getMessage();
        if (!(m instanceof ChannelBuffer)) {
            ctx.sendUpstream(e);
            return;
        }
        ChannelBuffer buffer = (ChannelBuffer) m;
        int size = buffer.getInt(buffer.readerIndex() - 4);
        transportServiceAdapter.received(size + 4);

        int markedReaderIndex = buffer.readerIndex();
        int expectedIndexReader = markedReaderIndex + size;

        // netty always copies a buffer, either in NioWorker in its read handler, where it copies to a fresh
        // buffer, or in the cumlation buffer, which is cleaned each time
        StreamInput streamIn = ChannelBufferStreamInputFactory.create(buffer, size);

        long requestId = buffer.readLong();
        byte status = buffer.readByte();
        Version version = Version.fromId(buffer.readInt());

        // !!! compression handling removed !!!
        StreamInput wrappedStream = CachedStreamInput.cachedHandles(streamIn);

        if (TransportStatus.isRequest(status)) {
            String action = handleRequest(ctx.getChannel(), wrappedStream, requestId, version);
            if (buffer.readerIndex() != expectedIndexReader) {
                if (buffer.readerIndex() < expectedIndexReader) {
                    logger.warn("Message not fully read (request) for [{}] and action [{}], resetting", requestId, action);
                } else {
                    logger.warn("Message read past expected size (request) for [{}] and action [{}], resetting", requestId, action);
                }
                buffer.readerIndex(expectedIndexReader);
            }
        } else {
            TransportResponseHandler handler = transportServiceAdapter.remove(requestId);
            // ignore if its null, the adapter logs it
            if (handler != null) {
                if (TransportStatus.isError(status)) {
                    handlerResponseError(wrappedStream, handler);
                } else {
                    handleResponse(wrappedStream, handler);
                }
            } else {
                // if its null, skip those bytes
                buffer.readerIndex(markedReaderIndex + size);
            }
            if (buffer.readerIndex() != expectedIndexReader) {
                if (buffer.readerIndex() < expectedIndexReader) {
                    logger.warn("Message not fully read (response) for [{}] handler {}, error [{}], resetting", requestId, handler, TransportStatus.isError(status));
                } else {
                    logger.warn("Message read past expected size (response) for [{}] handler {}, error [{}], resetting", requestId, handler, TransportStatus.isError(status));
                }
                buffer.readerIndex(expectedIndexReader);
            }
        }
        wrappedStream.close();
    }

    private void handleResponse(StreamInput buffer, final TransportResponseHandler handler) {
        final TransportResponse response = handler.newInstance();
        try {
            response.readFrom(buffer);
        } catch (Throwable e) {
            handleException(handler, new TransportSerializationException("Failed to deserialize response of type [" + response.getClass().getName() + "]", e));
            return;
        }
        try {
            if (handler.executor().equals(ThreadPool.Names.SAME)) {
                handler.handleResponse(response);
            } else {
                threadPool.executor(handler.executor()).execute(new ResponseHandler(handler, response));
            }
        } catch (Throwable e) {
            handleException(handler, new ResponseHandlerFailureTransportException(e));
        }
    }

    private void handlerResponseError(StreamInput buffer, final TransportResponseHandler handler) {
        Throwable error;
        try {
            ThrowableObjectInputStream ois = new ThrowableObjectInputStream(buffer, transport.settings().getClassLoader());
            error = (Throwable) ois.readObject();
        } catch (Exception e) {
            error = new TransportSerializationException("Failed to deserialize exception response from stream", e);
        }
        handleException(handler, error);
    }

    private void handleException(final TransportResponseHandler handler, Throwable error) {
        if (!(error instanceof RemoteTransportException)) {
            error = new RemoteTransportException(error.getMessage(), error);
        }
        final RemoteTransportException rtx = (RemoteTransportException) error;
        if (handler.executor().equals(ThreadPool.Names.SAME)) {
            handler.handleException(rtx);
        } else {
            threadPool.executor(handler.executor()).execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        handler.handleException(rtx);
                    } catch (Exception e) {
                        logger.error("Failed to handle exception response", e);
                    }
                }
            });
        }
    }

    private String handleRequest(Channel channel, StreamInput buffer, long requestId, Version version) throws IOException {
        final String action = buffer.readString();

        final NettyTransportChannel transportChannel =
                new NettyTransportChannel(transport, action, channel, requestId, version);
        try {
            final TransportRequestHandler handler = transportServiceAdapter.handler(action);
            if (handler == null) {
                throw new ActionNotFoundTransportException(action);
            }
            final TransportRequest request = handler.newInstance();
            request.readFrom(buffer);
            if (handler.executor().equals(ThreadPool.Names.SAME)) {
                //noinspection unchecked
                handler.messageReceived(request, transportChannel);
            } else {
                threadPool.executor(handler.executor()).execute(new RequestHandler(handler, request, transportChannel, action));
            }
        } catch (Throwable e) {
            try {
                transportChannel.sendResponse(e);
            } catch (IOException e1) {
                logger.warn("Failed to send error message back to client for action [" + action + "]", e);
                logger.warn("Actual Exception", e1);
            }
        }
        return action;
    }


    class ResponseHandler implements Runnable {

        private final TransportResponseHandler handler;
        private final TransportResponse response;

        public ResponseHandler(TransportResponseHandler handler, TransportResponse response) {
            this.handler = handler;
            this.response = response;
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public void run() {
            try {
                handler.handleResponse(response);
            } catch (Exception e) {
                handleException(handler, new ResponseHandlerFailureTransportException(e));
            }
        }
    }

    class RequestHandler implements Runnable {
        private final TransportRequestHandler handler;
        private final TransportRequest request;
        private final NettyTransportChannel transportChannel;
        private final String action;

        public RequestHandler(TransportRequestHandler handler, TransportRequest request, NettyTransportChannel transportChannel, String action) {
            this.handler = handler;
            this.request = request;
            this.transportChannel = transportChannel;
            this.action = action;
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public void run() {
            try {
                handler.messageReceived(request, transportChannel);
            } catch (Throwable e) {
                if (transport.lifecycleState() == Lifecycle.State.STARTED) {
                    // we can only send a response transport is started....
                    try {
                        transportChannel.sendResponse(e);
                    } catch (IOException e1) {
                        logger.warn("Failed to send error message back to client for action [" + action + "]", e1);
                        logger.warn("Actual Exception", e);
                    }
                }
            }
        }
    }
}
