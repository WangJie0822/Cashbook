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

package org.apache.http4.impl.io;

import java.io.IOException;

import org.apache.http4.HttpException;
import org.apache.http4.HttpMessage;
import org.apache.http4.HttpResponse;
import org.apache.http4.HttpResponseFactory;
import org.apache.http4.NoHttpResponseException;
import org.apache.http4.ParseException;
import org.apache.http4.StatusLine;
import org.apache.http4.io.SessionInputBuffer;
import org.apache.http4.message.LineParser;
import org.apache.http4.message.ParserCursor;
import org.apache.http4.params.HttpParams;
import org.apache.http4.util.Args;
import org.apache.http4.util.CharArrayBuffer;
import org.apache.http4.params.CoreConnectionPNames;

/**
 * HTTP response parser that obtain its input from an instance
 * of {@link SessionInputBuffer}.
 * <p>
 * The following parameters can be used to customize the behavior of this
 * class:
 * <ul>
 *  <li>{@link CoreConnectionPNames#MAX_HEADER_COUNT}</li>
 *  <li>{@link CoreConnectionPNames#MAX_LINE_LENGTH}</li>
 * </ul>
 *
 * @since 4.0
 *
 * @deprecated (4.2) use {@link DefaultHttpResponseParser}
 */
@Deprecated
public class HttpResponseParser extends AbstractMessageParser<HttpMessage> {

    private final HttpResponseFactory responseFactory;
    private final CharArrayBuffer lineBuf;

    /**
     * Creates an instance of this class.
     *
     * @param buffer the session input buffer.
     * @param parser the line parser.
     * @param responseFactory the factory to use to create
     *    {@link HttpResponse}s.
     * @param params HTTP parameters.
     */
    public HttpResponseParser(
            final SessionInputBuffer buffer,
            final LineParser parser,
            final HttpResponseFactory responseFactory,
            final HttpParams params) {
        super(buffer, parser, params);
        this.responseFactory = Args.notNull(responseFactory, "Response factory");
        this.lineBuf = new CharArrayBuffer(128);
    }

    @Override
    protected HttpMessage parseHead(
            final SessionInputBuffer sessionBuffer)
        throws IOException, HttpException, ParseException {

        this.lineBuf.clear();
        final int readLen = sessionBuffer.readLine(this.lineBuf);
        if (readLen == -1) {
            throw new NoHttpResponseException("The target server failed to respond");
        }
        //create the status line from the status string
        final ParserCursor cursor = new ParserCursor(0, this.lineBuf.length());
        final StatusLine statusline = lineParser.parseStatusLine(this.lineBuf, cursor);
        return this.responseFactory.newHttpResponse(statusline, null);
    }

}
