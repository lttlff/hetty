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
package com.zjhcsoft.hetty.http;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zjhcsoft.hetty.util.StringUtil;


/**
 * The default {@link HttpMessage} implementation.
 *
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author Andy Taylor (andy.taylor@jboss.org)
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 * @version $Rev: 2088 $, $Date: 2010-01-27 11:38:17 +0900 (Wed, 27 Jan 2010) $
 */
public class DefaultHttpMessage implements HttpMessage {

    private final HttpHeaders headers = new HttpHeaders();
    private HttpVersion version;
    private byte[] content = new byte[0];;
    private boolean chunked;

    /**
     * Creates a new instance.
     */
    protected DefaultHttpMessage(final HttpVersion version) {
        setProtocolVersion(version);
    }

    public void addHeader(final String name, final Object value) {
        headers.addHeader(name, value);
    }

    public void setHeader(final String name, final Object value) {
        headers.setHeader(name, value);
    }

    public void setHeader(final String name, final Iterable<?> values) {
        headers.setHeader(name, values);
    }

    public void removeHeader(final String name) {
        headers.removeHeader(name);
    }

    @Deprecated
    public long getContentLength() {
        return HttpHeaders.getContentLength(this);
    }

    @Deprecated
    public long getContentLength(long defaultValue) {
        return HttpHeaders.getContentLength(this, defaultValue);
    }

    public boolean isChunked() {
        if (chunked) {
            return true;
        } else {
            return HttpCodecUtil.isTransferEncodingChunked(this);
        }
    }

    public void setChunked(boolean chunked) {
        this.chunked = chunked;
        if (chunked) {
            setContent(new byte[0]);
        }
    }

    @Deprecated
    public boolean isKeepAlive() {
        return HttpHeaders.isKeepAlive(this);
    }

    public void clearHeaders() {
        headers.clearHeaders();
    }

    public void setContent(byte[] content) {
        if (content == null) {
            content = new byte[0];;
        }
        if (content.length > 0 && isChunked()) {
            throw new IllegalArgumentException(
                    "non-empty content disallowed if this.chunked == true");
        }
        this.content = content;
    }

    public String getHeader(final String name) {
        List<String> values = getHeaders(name);
        return values.size() > 0 ? values.get(0) : null;
    }

    public List<String> getHeaders(final String name) {
        return headers.getHeaders(name);
    }

    public List<Map.Entry<String, String>> getHeaders() {
        return headers.getHeaders();
    }

    public boolean containsHeader(final String name) {
        return headers.containsHeader(name);
    }

    public Set<String> getHeaderNames() {
        return headers.getHeaderNames();
    }

    public HttpVersion getProtocolVersion() {
        return version;
    }

    public void setProtocolVersion(HttpVersion version) {
        if (version == null) {
            throw new NullPointerException("version");
        }
        this.version = version;
    }

    public byte[] getContent() {
        return content;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append("(version: ");
        buf.append(getProtocolVersion().getText());
        buf.append(", keepAlive: ");
        buf.append(isKeepAlive());
        buf.append(", chunked: ");
        buf.append(isChunked());
        buf.append(')');
        buf.append(StringUtil.NEWLINE);
        appendHeaders(buf);

        // Remove the last newline.
        buf.setLength(buf.length() - StringUtil.NEWLINE.length());
        return buf.toString();
    }

    void appendHeaders(StringBuilder buf) {
        for (Map.Entry<String, String> e: getHeaders()) {
            buf.append(e.getKey());
            buf.append(": ");
            buf.append(e.getValue());
            buf.append(StringUtil.NEWLINE);
        }
    }
}
