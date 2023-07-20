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
package org.apache.http4.impl.conn;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.http4.conn.ClientConnectionOperator;
import org.apache.http4.conn.OperatedClientConnection;
import org.apache.http4.conn.routing.HttpRoute;
import org.apache.http4.pool.AbstractConnPool;
import org.apache.http4.pool.ConnFactory;

/**
 * @since 4.2
 *
 * @deprecated (4.3) no longer used.
 */
@Deprecated
class HttpConnPool extends AbstractConnPool<HttpRoute, OperatedClientConnection, HttpPoolEntry> {

    private static final AtomicLong COUNTER = new AtomicLong();

    private final Log log;
    private final long timeToLive;
    private final TimeUnit timeUnit;

    public HttpConnPool(final Log log,
            final ClientConnectionOperator connOperator,
            final int defaultMaxPerRoute, final int maxTotal,
            final long timeToLive, final TimeUnit timeUnit) {
        super(new InternalConnFactory(connOperator), defaultMaxPerRoute, maxTotal);
        this.log = log;
        this.timeToLive = timeToLive;
        this.timeUnit = timeUnit;
    }

    @Override
    protected HttpPoolEntry createEntry(final HttpRoute route, final OperatedClientConnection conn) {
        final String id = Long.toString(COUNTER.getAndIncrement());
        return new HttpPoolEntry(this.log, id, route, conn, this.timeToLive, this.timeUnit);
    }

    static class InternalConnFactory implements ConnFactory<HttpRoute, OperatedClientConnection> {

        private final ClientConnectionOperator connOperator;

        InternalConnFactory(final ClientConnectionOperator connOperator) {
            this.connOperator = connOperator;
        }

        @Override
        public OperatedClientConnection create(final HttpRoute route) throws IOException {
            return connOperator.createConnection();
        }

    }

}
