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
import java.net.Socket;

import org.apache.http4.io.EofSensor;
import org.apache.http4.params.HttpParams;
import org.apache.http4.util.Args;
import org.apache.http4.io.SessionInputBuffer;

/**
 * {@link SessionInputBuffer} implementation
 * bound to a {@link Socket}.
 *
 * @since 4.0
 *
 * @deprecated (4.3) use {@link SessionInputBufferImpl}
 */
@Deprecated
public class SocketInputBuffer extends AbstractSessionInputBuffer implements EofSensor {

    private final Socket socket;

    private boolean eof;

    /**
     * Creates an instance of this class.
     *
     * @param socket the socket to read data from.
     * @param bufferSize the size of the internal buffer. If this number is less
     *   than {@code 0} it is set to the value of
     *   {@link Socket#getReceiveBufferSize()}. If resultant number is less
     *   than {@code 1024} it is set to {@code 1024}.
     * @param params HTTP parameters.
     */
    public SocketInputBuffer(
            final Socket socket,
            final int bufferSize,
            final HttpParams params) throws IOException {
        super();
        Args.notNull(socket, "Socket");
        this.socket = socket;
        this.eof = false;
        int n = bufferSize;
        if (n < 0) {
            n = socket.getReceiveBufferSize();
        }
        if (n < 1024) {
            n = 1024;
        }
        init(socket.getInputStream(), n, params);
    }

    @Override
    protected int fillBuffer() throws IOException {
        final int i = super.fillBuffer();
        this.eof = i == -1;
        return i;
    }

    @Override
    public boolean isDataAvailable(final int timeout) throws IOException {
        boolean result = hasBufferedData();
        if (!result) {
            final int oldtimeout = this.socket.getSoTimeout();
            try {
                this.socket.setSoTimeout(timeout);
                fillBuffer();
                result = hasBufferedData();
            } finally {
                socket.setSoTimeout(oldtimeout);
            }
        }
        return result;
    }

    @Override
    public boolean isEof() {
        return this.eof;
    }

}
