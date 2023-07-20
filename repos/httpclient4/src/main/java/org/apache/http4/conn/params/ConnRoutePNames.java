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
package org.apache.http4.conn.params;

import org.apache.http4.HttpHost;
import org.apache.http4.client.config.RequestConfig;
import org.apache.http4.conn.routing.HttpRoute;
import org.apache.http4.conn.routing.HttpRoutePlanner;

/**
 * Parameter names for connection routing.
 *
 * @since 4.0
 *
 * @deprecated (4.3) use {@link RequestConfig}.
 */
@Deprecated
public interface ConnRoutePNames {

    /**
     * Parameter for the default proxy.
     * The default value will be used by some
     * {@link HttpRoutePlanner HttpRoutePlanner}
     * implementations, in particular the default implementation.
     * <p>
     * This parameter expects a value of type {@link HttpHost}.
     * </p>
     */
    String DEFAULT_PROXY = "http.route.default-proxy";

    /**
     * Parameter for the local address.
     * On machines with multiple network interfaces, this parameter
     * can be used to select the network interface from which the
     * connection originates.
     * It will be interpreted by the standard
     * {@link HttpRoutePlanner HttpRoutePlanner}
     * implementations, in particular the default implementation.
     * <p>
     * This parameter expects a value of type {@link java.net.InetAddress}.
     * </p>
     */
    String LOCAL_ADDRESS = "http.route.local-address";

    /**
     * Parameter for an forced route.
     * The forced route will be interpreted by the standard
     * {@link HttpRoutePlanner HttpRoutePlanner}
     * implementations.
     * Instead of computing a route, the given forced route will be
     * returned, even if it points to the wrong target host.
     * <p>
     * This parameter expects a value of type
     * {@link HttpRoute HttpRoute}.
     * </p>
     */
    String FORCED_ROUTE = "http.route.forced-route";

}

