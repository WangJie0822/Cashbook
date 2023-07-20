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

package org.apache.http4.impl.client;

import java.net.ProxySelector;

import org.apache.http4.ConnectionReuseStrategy;
import org.apache.http4.annotation.Contract;
import org.apache.http4.annotation.ThreadingBehavior;
import org.apache.http4.conn.ClientConnectionManager;
import org.apache.http4.conn.params.ConnRoutePNames;
import org.apache.http4.conn.routing.HttpRoutePlanner;
import org.apache.http4.impl.DefaultConnectionReuseStrategy;
import org.apache.http4.impl.NoConnectionReuseStrategy;
import org.apache.http4.impl.conn.PoolingClientConnectionManager;
import org.apache.http4.impl.conn.ProxySelectorRoutePlanner;
import org.apache.http4.impl.conn.SchemeRegistryFactory;
import org.apache.http4.params.HttpParams;
import org.apache.http4.auth.params.AuthPNames;
import org.apache.http4.client.params.ClientPNames;
import org.apache.http4.cookie.params.CookieSpecPNames;
import org.apache.http4.params.CoreConnectionPNames;
import org.apache.http4.params.CoreProtocolPNames;

/**
 * An extension of {@link DefaultHttpClient} pre-configured using system properties.
 * <p>
 * The following system properties are taken into account by this class:
 * <ul>
 *  <li>ssl.TrustManagerFactory.algorithm</li>
 *  <li>javax.net.ssl.trustStoreType</li>
 *  <li>javax.net.ssl.trustStore</li>
 *  <li>javax.net.ssl.trustStoreProvider</li>
 *  <li>javax.net.ssl.trustStorePassword</li>
 *  <li>java.home</li>
 *  <li>ssl.KeyManagerFactory.algorithm</li>
 *  <li>javax.net.ssl.keyStoreType</li>
 *  <li>javax.net.ssl.keyStore</li>
 *  <li>javax.net.ssl.keyStoreProvider</li>
 *  <li>javax.net.ssl.keyStorePassword</li>
 *  <li>http.proxyHost</li>
 *  <li>http.proxyPort</li>
 *  <li>https.proxyHost</li>
 *  <li>https.proxyPort</li>
 *  <li>http.nonProxyHosts</li>
 *  <li>http.keepAlive</li>
 *  <li>http.maxConnections</li>
 * </ul>
 * <p>
 * <p>
 * The following parameters can be used to customize the behavior of this
 * class:
 * </p>
 * <ul>
 *  <li>{@link CoreProtocolPNames#PROTOCOL_VERSION}</li>
 *  <li>{@link CoreProtocolPNames#STRICT_TRANSFER_ENCODING}</li>
 *  <li>{@link CoreProtocolPNames#HTTP_ELEMENT_CHARSET}</li>
 *  <li>{@link CoreProtocolPNames#USE_EXPECT_CONTINUE}</li>
 *  <li>{@link CoreProtocolPNames#WAIT_FOR_CONTINUE}</li>
 *  <li>{@link CoreProtocolPNames#USER_AGENT}</li>
 *  <li>{@link CoreConnectionPNames#TCP_NODELAY}</li>
 *  <li>{@link CoreConnectionPNames#SO_TIMEOUT}</li>
 *  <li>{@link CoreConnectionPNames#SO_LINGER}</li>
 *  <li>{@link CoreConnectionPNames#SO_REUSEADDR}</li>
 *  <li>{@link CoreConnectionPNames#SOCKET_BUFFER_SIZE}</li>
 *  <li>{@link CoreConnectionPNames#CONNECTION_TIMEOUT}</li>
 *  <li>{@link CoreConnectionPNames#MAX_LINE_LENGTH}</li>
 *  <li>{@link CoreConnectionPNames#MAX_HEADER_COUNT}</li>
 *  <li>{@link CoreConnectionPNames#STALE_CONNECTION_CHECK}</li>
 *  <li>{@link ConnRoutePNames#FORCED_ROUTE}</li>
 *  <li>{@link ConnRoutePNames#LOCAL_ADDRESS}</li>
 *  <li>{@link ConnRoutePNames#DEFAULT_PROXY}</li>
 *  <li>{@link CookieSpecPNames#DATE_PATTERNS}</li>
 *  <li>{@link CookieSpecPNames#SINGLE_COOKIE_HEADER}</li>
 *  <li>{@link AuthPNames#CREDENTIAL_CHARSET}</li>
 *  <li>{@link ClientPNames#COOKIE_POLICY}</li>
 *  <li>{@link ClientPNames#HANDLE_AUTHENTICATION}</li>
 *  <li>{@link ClientPNames#HANDLE_REDIRECTS}</li>
 *  <li>{@link ClientPNames#MAX_REDIRECTS}</li>
 *  <li>{@link ClientPNames#ALLOW_CIRCULAR_REDIRECTS}</li>
 *  <li>{@link ClientPNames#VIRTUAL_HOST}</li>
 *  <li>{@link ClientPNames#DEFAULT_HOST}</li>
 *  <li>{@link ClientPNames#DEFAULT_HEADERS}</li>
 *  <li>{@link ClientPNames#CONN_MANAGER_TIMEOUT}</li>
 * </ul>
 *
 * @since 4.2
 *
 * @deprecated (4.3) use {@link HttpClientBuilder}
 */
@Contract(threading = ThreadingBehavior.SAFE_CONDITIONAL)
@Deprecated
public class SystemDefaultHttpClient extends DefaultHttpClient {

    public SystemDefaultHttpClient(final HttpParams params) {
        super(null, params);
    }

    public SystemDefaultHttpClient() {
        super(null, null);
    }

    @Override
    protected ClientConnectionManager createClientConnectionManager() {
        final PoolingClientConnectionManager connmgr = new PoolingClientConnectionManager(
                SchemeRegistryFactory.createSystemDefault());
        String s = System.getProperty("http.keepAlive", "true");
        if ("true".equalsIgnoreCase(s)) {
            s = System.getProperty("http.maxConnections", "5");
            final int max = Integer.parseInt(s);
            connmgr.setDefaultMaxPerRoute(max);
            connmgr.setMaxTotal(2 * max);
        }
        return connmgr;
    }

    @Override
    protected HttpRoutePlanner createHttpRoutePlanner() {
        return new ProxySelectorRoutePlanner(getConnectionManager().getSchemeRegistry(),
                ProxySelector.getDefault());
    }

    @Override
    protected ConnectionReuseStrategy createConnectionReuseStrategy() {
        final String s = System.getProperty("http.keepAlive", "true");
        if ("true".equalsIgnoreCase(s)) {
            return new DefaultConnectionReuseStrategy();
        } else {
            return new NoConnectionReuseStrategy();
        }
    }

}
