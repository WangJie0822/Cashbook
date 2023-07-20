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

package org.apache.http4.conn.routing;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.http4.HttpHost;
import org.apache.http4.annotation.Contract;
import org.apache.http4.annotation.ThreadingBehavior;
import org.apache.http4.util.Args;
import org.apache.http4.util.LangUtils;

/**
 * The route for a request.
 *
 * @since 4.0
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public final class HttpRoute implements RouteInfo, Cloneable {

    /** The target host to connect to. */
    private final HttpHost targetHost;

    /**
     * The local address to connect from.
     * {@code null} indicates that the default should be used.
     */
    private final InetAddress localAddress;

    /** The proxy servers, if any. Never null. */
    private final List<HttpHost> proxyChain;

    /** Whether the the route is tunnelled through the proxy. */
    private final TunnelType tunnelled;

    /** Whether the route is layered. */
    private final LayerType layered;

    /** Whether the route is (supposed to be) secure. */
    private final boolean secure;

    private HttpRoute(final HttpHost target, final InetAddress local, final List<HttpHost> proxies,
                     final boolean secure, final TunnelType tunnelled, final LayerType layered) {
        Args.notNull(target, "Target host");
        this.targetHost = normalize(target);
        this.localAddress = local;
        if (proxies != null && !proxies.isEmpty()) {
            this.proxyChain = new ArrayList<HttpHost>(proxies);
        } else {
            this.proxyChain = null;
        }
        if (tunnelled == TunnelType.TUNNELLED) {
            Args.check(this.proxyChain != null, "Proxy required if tunnelled");
        }
        this.secure       = secure;
        this.tunnelled    = tunnelled != null ? tunnelled : TunnelType.PLAIN;
        this.layered      = layered != null ? layered : LayerType.PLAIN;
    }

    //TODO: to be removed in 5.0
    private static int getDefaultPort(final String schemeName) {
        if ("http".equalsIgnoreCase(schemeName)) {
            return 80;
        } else if ("https".equalsIgnoreCase(schemeName)) {
            return 443;
        } else {
            return -1;
        }

    }

    //TODO: to be removed in 5.0
    private static HttpHost normalize(final HttpHost target) {
        if (target.getPort() >= 0 ) {
            return target;
        }
        final InetAddress address = target.getAddress();
        final String schemeName = target.getSchemeName();
        return address != null
                        ? new HttpHost(address, getDefaultPort(schemeName), schemeName)
                        : new HttpHost(target.getHostName(), getDefaultPort(schemeName),
                                        schemeName);
    }

    /**
     * Creates a new route with all attributes specified explicitly.
     *
     * @param target    the host to which to route
     * @param local     the local address to route from, or
     *                  {@code null} for the default
     * @param proxies   the proxy chain to use, or
     *                  {@code null} for a direct route
     * @param secure    {@code true} if the route is (to be) secure,
     *                  {@code false} otherwise
     * @param tunnelled the tunnel type of this route
     * @param layered   the layering type of this route
     */
    public HttpRoute(final HttpHost target, final InetAddress local, final HttpHost[] proxies,
                     final boolean secure, final TunnelType tunnelled, final LayerType layered) {
        this(target, local, proxies != null ? Arrays.asList(proxies) : null,
                secure, tunnelled, layered);
    }

    /**
     * Creates a new route with at most one proxy.
     *
     * @param target    the host to which to route
     * @param local     the local address to route from, or
     *                  {@code null} for the default
     * @param proxy     the proxy to use, or
     *                  {@code null} for a direct route
     * @param secure    {@code true} if the route is (to be) secure,
     *                  {@code false} otherwise
     * @param tunnelled {@code true} if the route is (to be) tunnelled
     *                  via the proxy,
     *                  {@code false} otherwise
     * @param layered   {@code true} if the route includes a
     *                  layered protocol,
     *                  {@code false} otherwise
     */
    public HttpRoute(final HttpHost target, final InetAddress local, final HttpHost proxy,
                     final boolean secure, final TunnelType tunnelled, final LayerType layered) {
        this(target, local, proxy != null ? Collections.singletonList(proxy) : null,
                secure, tunnelled, layered);
    }

    /**
     * Creates a new direct route.
     * That is a route without a proxy.
     *
     * @param target    the host to which to route
     * @param local     the local address to route from, or
     *                  {@code null} for the default
     * @param secure    {@code true} if the route is (to be) secure,
     *                  {@code false} otherwise
     */
    public HttpRoute(final HttpHost target, final InetAddress local, final boolean secure) {
        this(target, local, Collections.<HttpHost>emptyList(), secure,
                TunnelType.PLAIN, LayerType.PLAIN);
    }

    /**
     * Creates a new direct insecure route.
     *
     * @param target    the host to which to route
     */
    public HttpRoute(final HttpHost target) {
        this(target, null, Collections.<HttpHost>emptyList(), false,
                TunnelType.PLAIN, LayerType.PLAIN);
    }

    /**
     * Creates a new route through a proxy.
     * When using this constructor, the {@code proxy} MUST be given.
     * For convenience, it is assumed that a secure connection will be
     * layered over a tunnel through the proxy.
     *
     * @param target    the host to which to route
     * @param local     the local address to route from, or
     *                  {@code null} for the default
     * @param proxy     the proxy to use
     * @param secure    {@code true} if the route is (to be) secure,
     *                  {@code false} otherwise
     */
    public HttpRoute(final HttpHost target, final InetAddress local, final HttpHost proxy,
                     final boolean secure) {
        this(target, local, Collections.singletonList(Args.notNull(proxy, "Proxy host")), secure,
             secure ? TunnelType.TUNNELLED : TunnelType.PLAIN,
             secure ? LayerType.LAYERED    : LayerType.PLAIN);
    }

    /**
     * Creates a new plain route through a proxy.
     *
     * @param target    the host to which to route
     * @param proxy     the proxy to use
     *
     * @since 4.3
     */
    public HttpRoute(final HttpHost target, final HttpHost proxy) {
        this(target, null, proxy, false);
    }

    @Override
    public HttpHost getTargetHost() {
        return this.targetHost;
    }

    @Override
    public InetAddress getLocalAddress() {
        return this.localAddress;
    }

    public InetSocketAddress getLocalSocketAddress() {
        return this.localAddress != null ? new InetSocketAddress(this.localAddress, 0) : null;
    }

    @Override
    public int getHopCount() {
        return proxyChain != null ? proxyChain.size() + 1 : 1;
    }

    @Override
    public HttpHost getHopTarget(final int hop) {
        Args.notNegative(hop, "Hop index");
        final int hopcount = getHopCount();
        Args.check(hop < hopcount, "Hop index exceeds tracked route length");
        return hop < hopcount - 1 ? this.proxyChain.get(hop) : this.targetHost;
    }

    @Override
    public HttpHost getProxyHost() {
        return proxyChain != null && !this.proxyChain.isEmpty() ? this.proxyChain.get(0) : null;
    }

    @Override
    public TunnelType getTunnelType() {
        return this.tunnelled;
    }

    @Override
    public boolean isTunnelled() {
        return (this.tunnelled == TunnelType.TUNNELLED);
    }

    @Override
    public LayerType getLayerType() {
        return this.layered;
    }

    @Override
    public boolean isLayered() {
        return (this.layered == LayerType.LAYERED);
    }

    @Override
    public boolean isSecure() {
        return this.secure;
    }

    /**
     * Compares this route to another.
     *
     * @param obj         the object to compare with
     *
     * @return  {@code true} if the argument is the same route,
     *          {@code false}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof HttpRoute) {
            final HttpRoute that = (HttpRoute) obj;
            return
                // Do the cheapest tests first
                (this.secure    == that.secure) &&
                (this.tunnelled == that.tunnelled) &&
                (this.layered   == that.layered) &&
                LangUtils.equals(this.targetHost, that.targetHost) &&
                LangUtils.equals(this.localAddress, that.localAddress) &&
                LangUtils.equals(this.proxyChain, that.proxyChain);
        }
        return false;
    }


    /**
     * Generates a hash code for this route.
     *
     * @return  the hash code
     */
    @Override
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, this.targetHost);
        hash = LangUtils.hashCode(hash, this.localAddress);
        if (this.proxyChain != null) {
            for (final HttpHost element : this.proxyChain) {
                hash = LangUtils.hashCode(hash, element);
            }
        }
        hash = LangUtils.hashCode(hash, this.secure);
        hash = LangUtils.hashCode(hash, this.tunnelled);
        hash = LangUtils.hashCode(hash, this.layered);
        return hash;
    }

    /**
     * Obtains a description of this route.
     *
     * @return  a human-readable representation of this route
     */
    @Override
    public String toString() {
        final StringBuilder cab = new StringBuilder(50 + getHopCount()*30);
        if (this.localAddress != null) {
            cab.append(this.localAddress);
            cab.append("->");
        }
        cab.append('{');
        if (this.tunnelled == TunnelType.TUNNELLED) {
            cab.append('t');
        }
        if (this.layered == LayerType.LAYERED) {
            cab.append('l');
        }
        if (this.secure) {
            cab.append('s');
        }
        cab.append("}->");
        if (this.proxyChain != null) {
            for (final HttpHost aProxyChain : this.proxyChain) {
                cab.append(aProxyChain);
                cab.append("->");
            }
        }
        cab.append(this.targetHost);
        return cab.toString();
    }

    // default implementation of clone() is sufficient
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
