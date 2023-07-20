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

package org.apache.http4.protocol;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.http4.HttpConnection;
import org.apache.http4.HttpException;
import org.apache.http4.HttpHost;
import org.apache.http4.HttpInetConnection;
import org.apache.http4.HttpRequest;
import org.apache.http4.HttpRequestInterceptor;
import org.apache.http4.HttpVersion;
import org.apache.http4.ProtocolException;
import org.apache.http4.ProtocolVersion;
import org.apache.http4.annotation.ThreadingBehavior;
import org.apache.http4.annotation.Contract;
import org.apache.http4.util.Args;

/**
 * RequestTargetHost is responsible for adding {@code Host} header. This
 * interceptor is required for client side protocol processors.
 *
 * @since 4.0
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class RequestTargetHost implements HttpRequestInterceptor {

    public RequestTargetHost() {
        super();
    }

    @Override
    public void process(final HttpRequest request, final HttpContext context)
            throws HttpException, IOException {
        Args.notNull(request, "HTTP request");

        final HttpCoreContext coreContext = HttpCoreContext.adapt(context);

        final ProtocolVersion ver = request.getRequestLine().getProtocolVersion();
        final String method = request.getRequestLine().getMethod();
        if (method.equalsIgnoreCase("CONNECT") && ver.lessEquals(HttpVersion.HTTP_1_0)) {
            return;
        }

        if (!request.containsHeader(HTTP.TARGET_HOST)) {
            HttpHost targetHost = coreContext.getTargetHost();
            if (targetHost == null) {
                final HttpConnection conn = coreContext.getConnection();
                if (conn instanceof HttpInetConnection) {
                    // Populate the context with a default HTTP host based on the
                    // inet address of the target host
                    final InetAddress address = ((HttpInetConnection) conn).getRemoteAddress();
                    final int port = ((HttpInetConnection) conn).getRemotePort();
                    if (address != null) {
                        targetHost = new HttpHost(address.getHostName(), port);
                    }
                }
                if (targetHost == null) {
                    if (ver.lessEquals(HttpVersion.HTTP_1_0)) {
                        return;
                    }
                    throw new ProtocolException("Target host missing");
                }
            }
            request.addHeader(HTTP.TARGET_HOST, targetHost.toHostString());
        }
    }

}
