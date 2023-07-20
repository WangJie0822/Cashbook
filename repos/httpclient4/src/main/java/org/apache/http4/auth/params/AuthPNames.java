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

package org.apache.http4.auth.params;

import org.apache.http4.auth.AuthScheme;
import org.apache.http4.auth.AuthSchemeProvider;
import org.apache.http4.auth.Credentials;
import org.apache.http4.client.config.RequestConfig;

/**
 * Parameter names for HTTP authentication classes.
 *
 * @since 4.0
 *
 * @deprecated (4.3) use {@link RequestConfig}
 *   and constructor parameters of
 *   {@link AuthSchemeProvider}s.
*/
@Deprecated
public interface AuthPNames {

    /**
     * Defines the charset to be used when encoding
     * {@link Credentials}.
     * <p>
     * This parameter expects a value of type {@link String}.
     */
    String CREDENTIAL_CHARSET = "http.auth.credential-charset";

    /**
     * Defines the order of preference for supported
     *  {@link AuthScheme}s when authenticating with
     *  the target host.
     * <p>
     * This parameter expects a value of type {@link java.util.Collection}. The
     * collection is expected to contain {@link String} instances representing
     * a name of an authentication scheme as returned by
     * {@link AuthScheme#getSchemeName()}.
     */
    String TARGET_AUTH_PREF = "http.auth.target-scheme-pref";

    /**
     * Defines the order of preference for supported
     *  {@link AuthScheme}s when authenticating with the
     *  proxy host.
     * <p>
     * This parameter expects a value of type {@link java.util.Collection}. The
     * collection is expected to contain {@link String} instances representing
     * a name of an authentication scheme as returned by
     * {@link AuthScheme#getSchemeName()}.
     */
    String PROXY_AUTH_PREF = "http.auth.proxy-scheme-pref";

}
