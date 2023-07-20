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

package org.apache.http4.client.protocol;

import java.io.IOException;

import org.apache.http4.HttpEntity;
import org.apache.http4.HttpEntityEnclosingRequest;
import org.apache.http4.HttpException;
import org.apache.http4.HttpRequest;
import org.apache.http4.HttpRequestInterceptor;
import org.apache.http4.HttpVersion;
import org.apache.http4.ProtocolVersion;
import org.apache.http4.annotation.Contract;
import org.apache.http4.annotation.ThreadingBehavior;
import org.apache.http4.client.config.RequestConfig;
import org.apache.http4.protocol.HTTP;
import org.apache.http4.protocol.HttpContext;
import org.apache.http4.util.Args;

/**
 * RequestExpectContinue is responsible for enabling the 'expect-continue'
 * handshake by adding {@code Expect} header.
 * <p>
 * This interceptor takes into account {@link RequestConfig#isExpectContinueEnabled()}
 * setting.
 * </p>
 *
 * @since 4.3
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class RequestExpectContinue implements HttpRequestInterceptor {

    public RequestExpectContinue() {
        super();
    }

    @Override
    public void process(final HttpRequest request, final HttpContext context)
            throws HttpException, IOException {
        Args.notNull(request, "HTTP request");

        if (!request.containsHeader(HTTP.EXPECT_DIRECTIVE)) {
            if (request instanceof HttpEntityEnclosingRequest) {
                final ProtocolVersion ver = request.getRequestLine().getProtocolVersion();
                final HttpEntity entity = ((HttpEntityEnclosingRequest)request).getEntity();
                // Do not send the expect header if request body is known to be empty
                if (entity != null
                        && entity.getContentLength() != 0 && !ver.lessEquals(HttpVersion.HTTP_1_0)) {
                    final HttpClientContext clientContext = HttpClientContext.adapt(context);
                    final RequestConfig config = clientContext.getRequestConfig();
                    if (config.isExpectContinueEnabled()) {
                        request.addHeader(HTTP.EXPECT_DIRECTIVE, HTTP.EXPECT_CONTINUE);
                    }
                }
            }
        }
    }

}
