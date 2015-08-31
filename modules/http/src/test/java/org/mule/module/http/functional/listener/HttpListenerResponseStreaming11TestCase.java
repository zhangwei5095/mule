/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import org.apache.http.HttpVersion;
import org.junit.Test;

public class HttpListenerResponseStreaming11TestCase extends HttpListenerResponseStreamingTestCase
{

    @Override
    protected HttpVersion getHttpVersion()
    {
        return HttpVersion.HTTP_1_1;
    }

    // AUTO - String

    @Test
    public void string() throws Exception
    {
        final String url = getUrl("string");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void stringWithContentLengthHeader() throws Exception
    {
        final String url = getUrl("stringWithContentLengthHeader");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void stringWithContentLengthOutboundProperty() throws Exception
    {
        final String url = getUrl("stringWithContentLengthOutboundProperty");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void stringWithTransferEncoding() throws Exception
    {
        final String url = getUrl("stringWithTransferEncoding");
        testResponseIsChunkedEncoding(url, getHttpVersion());
    }

    @Test
    public void stringWithTransferEncodingOutboundProperty() throws Exception
    {
        final String url = getUrl("stringWithTransferEncodingOutboundProperty");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    // AUTO  - InputStream

    @Test
    public void inputStream() throws Exception
    {
        final String url = getUrl("inputStream");
        testResponseIsChunkedEncoding(url, getHttpVersion());
    }

    @Test
    public void inputStreamWithContentLengthHeader() throws Exception
    {
        final String url = getUrl("inputStreamWithContentLengthHeader");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void inputStreamWithContentLengthOutboundProperty() throws Exception
    {
        final String url = getUrl("inputStreamWithContentLengthOutboundProperty");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void inputStreamWithTransferEncoding() throws Exception
    {
        final String url = getUrl("inputStreamWithTransferEncoding");
        testResponseIsChunkedEncoding(url, getHttpVersion());
    }

    @Test
    public void inputStreamWithTransferEncodingOutboundProperty() throws Exception
    {
        final String url = getUrl("inputStreamWithTransferEncodingOutboundProperty");
        testResponseIsChunkedEncoding(url, getHttpVersion());
    }

    @Test
    public void inputStreamWithTransferEncodingAndContentLength() throws Exception
    {
        final String url = getUrl("inputStreamWithTransferEncodingAndContentLength");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    // NEVER - String

    @Test
    public void neverString() throws Exception
    {
        final String url = getUrl("neverString");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void neverStringTransferEncodingHeader() throws Exception
    {
        final String url = getUrl("neverStringTransferEncodingHeader");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void neverStringTransferEncodingOutboundProperty() throws Exception
    {
        final String url = getUrl("neverStringTransferEncodingOutboundProperty");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    // NEVER - InputStream

    @Test
    public void neverInputStream() throws Exception
    {
        final String url = getUrl("neverInputStream");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void neverInputStreamTransferEncodingHeader() throws Exception
    {
        final String url = getUrl("neverInputStreamTransferEncodingHeader");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    @Test
    public void neverInputStreamTransferEncodingOutboundProperty() throws Exception
    {
        final String url = getUrl("neverInputStreamTransferEncodingOutboundProperty");
        testResponseIsContentLengthEncoding(url, getHttpVersion());
    }

    // ALWAYS - String

    @Test
    public void alwaysString() throws Exception
    {
        final String url = getUrl("alwaysString");
        testResponseIsChunkedEncoding(url, getHttpVersion());
    }

    @Test
    public void alwaysStringContentLengthHeader() throws Exception
    {
        final String url = getUrl("alwaysStringContentLengthHeader");
        testResponseIsChunkedEncoding(url, getHttpVersion());
    }

    @Test
    public void alwaysStringContentLengthOutboundProperty() throws Exception
    {
        final String url = getUrl("alwaysStringContentLengthOutboundProperty");
        testResponseIsChunkedEncoding(url, getHttpVersion());
    }

    // ALWAYS - InputStream

    @Test
    public void alwaysInputStream() throws Exception
    {
        final String url = getUrl("alwaysInputStream");
        testResponseIsChunkedEncoding(url, getHttpVersion());
    }

    @Test
    public void alwaysInputStreamContentLengthHeader() throws Exception
    {
        final String url = getUrl("alwaysInputStreamContentLengthHeader");
        testResponseIsChunkedEncoding(url, getHttpVersion());
    }

    @Test
    public void alwaysInputStreamContentLengthOutboundProperty() throws Exception
    {
        final String url = getUrl("alwaysInputStreamContentLengthOutboundProperty");
        testResponseIsChunkedEncoding(url, getHttpVersion());
    }

}
