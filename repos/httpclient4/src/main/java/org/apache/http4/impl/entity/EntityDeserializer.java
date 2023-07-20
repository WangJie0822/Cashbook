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

package org.apache.http4.impl.entity;

import java.io.IOException;

import org.apache.http4.Header;
import org.apache.http4.HttpEntity;
import org.apache.http4.HttpException;
import org.apache.http4.HttpMessage;
import org.apache.http4.annotation.ThreadingBehavior;
import org.apache.http4.annotation.Contract;
import org.apache.http4.entity.BasicHttpEntity;
import org.apache.http4.entity.ContentLengthStrategy;
import org.apache.http4.impl.BHttpConnectionBase;
import org.apache.http4.impl.io.ChunkedInputStream;
import org.apache.http4.impl.io.ContentLengthInputStream;
import org.apache.http4.impl.io.IdentityInputStream;
import org.apache.http4.io.SessionInputBuffer;
import org.apache.http4.protocol.HTTP;
import org.apache.http4.util.Args;

/**
 * HTTP entity deserializer.
 * <p>
 * This entity deserializer supports "chunked" and "identitiy" transfer-coding
 * and content length delimited content.
 * <p>
 * This class relies on a specific implementation of
 * {@link ContentLengthStrategy} to determine the content length or transfer
 * encoding of the entity.
 * <p>
 * This class generates an instance of {@link HttpEntity} based on
 * properties of the message. The content of the entity will be decoded
 * transparently for the consumer.
 *
 * @since 4.0
 *
 * @deprecated (4.3) use {@link BHttpConnectionBase}
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE_CONDITIONAL)
@Deprecated
public class EntityDeserializer {

    private final ContentLengthStrategy lenStrategy;

    public EntityDeserializer(final ContentLengthStrategy lenStrategy) {
        super();
        this.lenStrategy = Args.notNull(lenStrategy, "Content length strategy");
    }

    /**
     * Creates a {@link BasicHttpEntity} based on properties of the given
     * message. The content of the entity is created by wrapping
     * {@link SessionInputBuffer} with a content decoder depending on the
     * transfer mechanism used by the message.
     * <p>
     * This method is called by the public
     * {@link #deserialize(SessionInputBuffer, HttpMessage)}.
     *
     * @param inBuffer the session input buffer.
     * @param message the message.
     * @return HTTP entity.
     * @throws HttpException in case of HTTP protocol violation.
     * @throws IOException in case of an I/O error.
     */
    protected BasicHttpEntity doDeserialize(
            final SessionInputBuffer inBuffer,
            final HttpMessage message) throws HttpException, IOException {
        final BasicHttpEntity entity = new BasicHttpEntity();

        final long len = this.lenStrategy.determineLength(message);
        if (len == ContentLengthStrategy.CHUNKED) {
            entity.setChunked(true);
            entity.setContentLength(-1);
            entity.setContent(new ChunkedInputStream(inBuffer));
        } else if (len == ContentLengthStrategy.IDENTITY) {
            entity.setChunked(false);
            entity.setContentLength(-1);
            entity.setContent(new IdentityInputStream(inBuffer));
        } else {
            entity.setChunked(false);
            entity.setContentLength(len);
            entity.setContent(new ContentLengthInputStream(inBuffer, len));
        }

        final Header contentTypeHeader = message.getFirstHeader(HTTP.CONTENT_TYPE);
        if (contentTypeHeader != null) {
            entity.setContentType(contentTypeHeader);
        }
        final Header contentEncodingHeader = message.getFirstHeader(HTTP.CONTENT_ENCODING);
        if (contentEncodingHeader != null) {
            entity.setContentEncoding(contentEncodingHeader);
        }
        return entity;
    }

    /**
     * Creates an {@link HttpEntity} based on properties of the given message.
     * The content of the entity is created by wrapping
     * {@link SessionInputBuffer} with a content decoder depending on the
     * transfer mechanism used by the message.
     * <p>
     * The content of the entity is NOT retrieved by this method.
     *
     * @param inBuffer the session input buffer.
     * @param message the message.
     * @return HTTP entity.
     * @throws HttpException in case of HTTP protocol violation.
     * @throws IOException in case of an I/O error.
     */
    public HttpEntity deserialize(
            final SessionInputBuffer inBuffer,
            final HttpMessage message) throws HttpException, IOException {
        Args.notNull(inBuffer, "Session input buffer");
        Args.notNull(message, "HTTP message");
        return doDeserialize(inBuffer, message);
    }

}
