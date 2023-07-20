/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http4.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import org.apache.http4.HttpClientConnection;
import org.apache.http4.HttpEntity;
import org.apache.http4.HttpEntityEnclosingRequest;
import org.apache.http4.HttpException;
import org.apache.http4.HttpRequest;
import org.apache.http4.HttpResponse;
import org.apache.http4.HttpStatus;
import org.apache.http4.config.MessageConstraints;
import org.apache.http4.entity.ContentLengthStrategy;
import org.apache.http4.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http4.impl.io.DefaultHttpResponseParserFactory;
import org.apache.http4.io.HttpMessageParser;
import org.apache.http4.io.HttpMessageParserFactory;
import org.apache.http4.io.HttpMessageWriter;
import org.apache.http4.io.HttpMessageWriterFactory;
import org.apache.http4.util.Args;
import org.apache.http4.impl.entity.LaxContentLengthStrategy;
import org.apache.http4.impl.entity.StrictContentLengthStrategy;

/**
 * Default implementation of {@link HttpClientConnection}.
 *
 * @since 4.3
 */
public class DefaultBHttpClientConnection extends BHttpConnectionBase
                                                   implements HttpClientConnection {

    private final HttpMessageParser<HttpResponse> responseParser;
    private final HttpMessageWriter<HttpRequest> requestWriter;

    /**
     * Creates new instance of DefaultBHttpClientConnection.
     *
     * @param bufferSize buffer size. Must be a positive number.
     * @param fragmentSizeHint fragment size hint.
     * @param charDecoder decoder to be used for decoding HTTP protocol elements.
     *   If {@code null} simple type cast will be used for byte to char conversion.
     * @param charEncoder encoder to be used for encoding HTTP protocol elements.
     *   If {@code null} simple type cast will be used for char to byte conversion.
     * @param constraints Message constraints. If {@code null}
     *   {@link MessageConstraints#DEFAULT} will be used.
     * @param incomingContentStrategy incoming content length strategy. If {@code null}
     *   {@link LaxContentLengthStrategy#INSTANCE} will be used.
     * @param outgoingContentStrategy outgoing content length strategy. If {@code null}
     *   {@link StrictContentLengthStrategy#INSTANCE} will be used.
     * @param requestWriterFactory request writer factory. If {@code null}
     *   {@link DefaultHttpRequestWriterFactory#INSTANCE} will be used.
     * @param responseParserFactory response parser factory. If {@code null}
     *   {@link DefaultHttpResponseParserFactory#INSTANCE} will be used.
     */
    public DefaultBHttpClientConnection(
            final int bufferSize,
            final int fragmentSizeHint,
            final CharsetDecoder charDecoder,
            final CharsetEncoder charEncoder,
            final MessageConstraints constraints,
            final ContentLengthStrategy incomingContentStrategy,
            final ContentLengthStrategy outgoingContentStrategy,
            final HttpMessageWriterFactory<HttpRequest> requestWriterFactory,
            final HttpMessageParserFactory<HttpResponse> responseParserFactory) {
        super(bufferSize, fragmentSizeHint, charDecoder, charEncoder,
                constraints, incomingContentStrategy, outgoingContentStrategy);
        this.requestWriter = (requestWriterFactory != null ? requestWriterFactory :
            DefaultHttpRequestWriterFactory.INSTANCE).create(getSessionOutputBuffer());
        this.responseParser = (responseParserFactory != null ? responseParserFactory :
            DefaultHttpResponseParserFactory.INSTANCE).create(getSessionInputBuffer(), constraints);
    }

    public DefaultBHttpClientConnection(
            final int bufferSize,
            final CharsetDecoder charDecoder,
            final CharsetEncoder charEncoder,
            final MessageConstraints constraints) {
        this(bufferSize, bufferSize, charDecoder, charEncoder, constraints, null, null, null, null);
    }

    public DefaultBHttpClientConnection(final int bufferSize) {
        this(bufferSize, bufferSize, null, null, null, null, null, null, null);
    }

    protected void onResponseReceived(final HttpResponse response) {
    }

    protected void onRequestSubmitted(final HttpRequest request) {
    }

    @Override
    public void bind(final Socket socket) throws IOException {
        super.bind(socket);
    }

    @Override
    public boolean isResponseAvailable(final int timeout) throws IOException {
        ensureOpen();
        try {
            return awaitInput(timeout);
        } catch (final SocketTimeoutException ex) {
            return false;
        }
    }

    @Override
    public void sendRequestHeader(final HttpRequest request)
            throws HttpException, IOException {
        Args.notNull(request, "HTTP request");
        ensureOpen();
        this.requestWriter.write(request);
        onRequestSubmitted(request);
        incrementRequestCount();
    }

    @Override
    public void sendRequestEntity(final HttpEntityEnclosingRequest request)
            throws HttpException, IOException {
        Args.notNull(request, "HTTP request");
        ensureOpen();
        final HttpEntity entity = request.getEntity();
        if (entity == null) {
            return;
        }
        final OutputStream outStream = prepareOutput(request);
        entity.writeTo(outStream);
        outStream.close();
    }

    @Override
    public HttpResponse receiveResponseHeader() throws HttpException, IOException {
        ensureOpen();
        final HttpResponse response = this.responseParser.parse();
        onResponseReceived(response);
        if (response.getStatusLine().getStatusCode() >= HttpStatus.SC_OK) {
            incrementResponseCount();
        }
        return response;
    }

    @Override
    public void receiveResponseEntity(
            final HttpResponse response) throws HttpException, IOException {
        Args.notNull(response, "HTTP response");
        ensureOpen();
        final HttpEntity entity = prepareInput(response);
        response.setEntity(entity);
    }

    @Override
    public void flush() throws IOException {
        ensureOpen();
        doFlush();
    }

}
