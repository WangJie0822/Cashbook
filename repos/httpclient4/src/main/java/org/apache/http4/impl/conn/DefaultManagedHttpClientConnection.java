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

package org.apache.http4.impl.conn;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http4.HttpRequest;
import org.apache.http4.HttpResponse;
import org.apache.http4.config.MessageConstraints;
import org.apache.http4.conn.ManagedHttpClientConnection;
import org.apache.http4.entity.ContentLengthStrategy;
import org.apache.http4.impl.DefaultBHttpClientConnection;
import org.apache.http4.io.HttpMessageParserFactory;
import org.apache.http4.io.HttpMessageWriterFactory;
import org.apache.http4.protocol.HttpContext;

/**
 * Default {@link ManagedHttpClientConnection} implementation.
 * @since 4.3
 */
public class DefaultManagedHttpClientConnection extends DefaultBHttpClientConnection
                                 implements ManagedHttpClientConnection, HttpContext {

    private final String id;
    private final Map<String, Object> attributes;

    private volatile boolean shutdown;

    public DefaultManagedHttpClientConnection(
            final String id,
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
                constraints, incomingContentStrategy, outgoingContentStrategy,
                requestWriterFactory, responseParserFactory);
        this.id = id;
        this.attributes = new ConcurrentHashMap<String, Object>();
    }

    public DefaultManagedHttpClientConnection(
            final String id,
            final int bufferSize) {
        this(id, bufferSize, bufferSize, null, null, null, null, null, null, null);
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void shutdown() throws IOException {
        this.shutdown = true;
        super.shutdown();
    }

    @Override
    public Object getAttribute(final String id) {
        return this.attributes.get(id);
    }

    @Override
    public Object removeAttribute(final String id) {
        return this.attributes.remove(id);
    }

    @Override
    public void setAttribute(final String id, final Object obj) {
        this.attributes.put(id, obj);
    }

    @Override
    public void bind(final Socket socket) throws IOException {
        if (this.shutdown) {
            socket.close(); // allow this to throw...
            // ...but if it doesn't, explicitly throw one ourselves.
            throw new InterruptedIOException("Connection already shutdown");
        }
        super.bind(socket);
    }

    @Override
    public Socket getSocket() {
        return super.getSocket();
    }

    @Override
    public SSLSession getSSLSession() {
        final Socket socket = super.getSocket();
        return socket instanceof SSLSocket ? ((SSLSocket) socket).getSession() : null;
    }

}
