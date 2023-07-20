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

import org.apache.http4.HttpRequest;
import org.apache.http4.HttpRequestFactory;
import org.apache.http4.annotation.ThreadingBehavior;
import org.apache.http4.annotation.Contract;
import org.apache.http4.config.MessageConstraints;
import org.apache.http4.impl.DefaultHttpRequestFactory;
import org.apache.http4.io.HttpMessageParser;
import org.apache.http4.io.HttpMessageParserFactory;
import org.apache.http4.io.SessionInputBuffer;
import org.apache.http4.message.BasicLineParser;
import org.apache.http4.message.LineParser;

/**
 * Default factory for request message parsers.
 *
 * @since 4.3
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE_CONDITIONAL)
public class DefaultHttpRequestParserFactory implements HttpMessageParserFactory<HttpRequest> {

    public static final DefaultHttpRequestParserFactory INSTANCE = new DefaultHttpRequestParserFactory();

    private final LineParser lineParser;
    private final HttpRequestFactory requestFactory;

    public DefaultHttpRequestParserFactory(final LineParser lineParser,
            final HttpRequestFactory requestFactory) {
        super();
        this.lineParser = lineParser != null ? lineParser : BasicLineParser.INSTANCE;
        this.requestFactory = requestFactory != null ? requestFactory
                : DefaultHttpRequestFactory.INSTANCE;
    }

    public DefaultHttpRequestParserFactory() {
        this(null, null);
    }

    @Override
    public HttpMessageParser<HttpRequest> create(final SessionInputBuffer buffer,
                                                 final MessageConstraints constraints) {
        return new DefaultHttpRequestParser(buffer, lineParser, requestFactory, constraints);
    }

}
