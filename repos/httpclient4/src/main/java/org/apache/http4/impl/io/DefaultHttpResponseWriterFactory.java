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

import org.apache.http4.HttpResponse;
import org.apache.http4.annotation.ThreadingBehavior;
import org.apache.http4.annotation.Contract;
import org.apache.http4.io.HttpMessageWriter;
import org.apache.http4.io.HttpMessageWriterFactory;
import org.apache.http4.io.SessionOutputBuffer;
import org.apache.http4.message.BasicLineFormatter;
import org.apache.http4.message.LineFormatter;

/**
 * Default factory for response message writers.
 *
 * @since 4.3
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE_CONDITIONAL)
public class DefaultHttpResponseWriterFactory implements HttpMessageWriterFactory<HttpResponse> {

    public static final DefaultHttpResponseWriterFactory INSTANCE = new DefaultHttpResponseWriterFactory();

    private final LineFormatter lineFormatter;

    public DefaultHttpResponseWriterFactory(final LineFormatter lineFormatter) {
        super();
        this.lineFormatter = lineFormatter != null ? lineFormatter : BasicLineFormatter.INSTANCE;
    }

    public DefaultHttpResponseWriterFactory() {
        this(null);
    }

    @Override
    public HttpMessageWriter<HttpResponse> create(final SessionOutputBuffer buffer) {
        return new DefaultHttpResponseWriter(buffer, lineFormatter);
    }

}
