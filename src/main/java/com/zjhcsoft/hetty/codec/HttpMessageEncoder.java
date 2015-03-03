/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.zjhcsoft.hetty.codec;

import static com.zjhcsoft.hetty.http.HttpCodecUtil.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.zjhcsoft.hetty.constant.Constant;
import com.zjhcsoft.hetty.http.HttpChunk;
import com.zjhcsoft.hetty.http.HttpChunkTrailer;
import com.zjhcsoft.hetty.http.HttpCodecUtil;
import com.zjhcsoft.hetty.http.HttpMessage;
import com.zjhcsoft.hetty.util.ByteBuffers;
import com.zjhcsoft.hetty.util.CharsetUtil;


/**
 * Encodes an {@link HttpMessage} or an {@link HttpChunk} into
 * a {@link ChannelBuffer}.
 *
 * <h3>Extensibility</h3>
 *
 * Please note that this encoder is designed to be extended to implement
 * a protocol derived from HTTP, such as
 * <a href="http://en.wikipedia.org/wiki/Real_Time_Streaming_Protocol">RTSP</a> and
 * <a href="http://en.wikipedia.org/wiki/Internet_Content_Adaptation_Protocol">ICAP</a>.
 * To implement the encoder of such a derived protocol, extend this class and
 * implement all abstract methods properly.
 *
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author Andy Taylor (andy.taylor@jboss.org)
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 * @version $Rev: 2121 $, $Date: 2010-02-02 09:38:07 +0900 (Tue, 02 Feb 2010) $
 *
 * @apiviz.landmark
 */
public abstract class HttpMessageEncoder {

    private static final ByteBuffer LAST_CHUNK =
    	ByteBuffer.wrap("0\r\n\r\n".getBytes(CharsetUtil.US_ASCII));

    private volatile boolean chunked;

    /**
     * Creates a new instance.
     */
    protected HttpMessageEncoder() {
        super();
    }

    protected Object encode( Object msg) throws Exception {
        if (msg instanceof HttpMessage) {
            HttpMessage m = (HttpMessage) msg;
            boolean chunked = this.chunked = HttpCodecUtil.isTransferEncodingChunked(m);
            ByteBuffer header = ByteBuffer.allocate(Constant.DEFAULT_BUFFER_SIZE);
            encodeInitialLine(header, m);
            encodeHeaders(header, m);
            header.put(CR);
            header.put(LF);

            byte[] content = m.getContent();
            if (content.length<1) {
                return header; // no content
            } else if (chunked) {
                throw new IllegalArgumentException(
                        "HttpMessage.content must be empty " +
                        "if Transfer-Encoding is chunked.");
            } else {
                return ByteBuffers.wrappedBuffer(header, ByteBuffer.wrap(content)).array();
            }
        }

        if (msg instanceof HttpChunk) {
            HttpChunk chunk = (HttpChunk) msg;
            if (chunked) {
                if (chunk.isLast()) {
                    chunked = false;
                    if (chunk instanceof HttpChunkTrailer) {
                    	ByteBuffer trailer = ByteBuffer.allocate(Constant.DEFAULT_BUFFER_SIZE);
                        trailer.put((byte) '0');
                        trailer.put(CR);
                        trailer.put(LF);
                        encodeTrailingHeaders(trailer, (HttpChunkTrailer) chunk);
                        trailer.put(CR);
                        trailer.put(LF);
                        return trailer;
                    } else {
                        return LAST_CHUNK.duplicate();
                    }
                } else {
                    byte[] content = chunk.getContent();
                    int contentLength = content.length;
                    ByteBuffer.allocate(Integer.toHexString(contentLength).length());
                    return wrappedBuffer(
                    		ByteBuffer(
                                    Integer.toHexString(contentLength),
                                    CharsetUtil.US_ASCII),
                            wrappedBuffer(CRLF),
                            content.slice(content.readerIndex(), contentLength),
                            wrappedBuffer(CRLF));
                }
            } else {
                if (chunk.isLast()) {
                    return null;
                } else {
                    return chunk.getContent();
                }
            }

        }

        // Unknown message type.
        return msg;
    }

    private void encodeHeaders(ByteBuffer buf, HttpMessage message) {
        try {
            for (Map.Entry<String, String> h: message.getHeaders()) {
                encodeHeader(buf, h.getKey(), h.getValue());
            }
        } catch (UnsupportedEncodingException e) {
            throw (Error) new Error().initCause(e);
        }
    }

    private void encodeTrailingHeaders(ByteBuffer buf, HttpChunkTrailer trailer) {
        try {
            for (Map.Entry<String, String> h: trailer.getHeaders()) {
                encodeHeader(buf, h.getKey(), h.getValue());
            }
        } catch (UnsupportedEncodingException e) {
            throw (Error) new Error().initCause(e);
        }
    }

    private void encodeHeader(ByteBuffer buf, String header, String value)
            throws UnsupportedEncodingException {
        buf.put(header.getBytes("ASCII"));
        buf.put(COLON);
        buf.put(SP);
        buf.put(value.getBytes("ASCII"));
        buf.put(CR);
        buf.put(LF);
    }

    protected abstract void encodeInitialLine(ByteBuffer buf, HttpMessage message) throws Exception;
}
