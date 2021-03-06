/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.Message.builder;
import static org.mule.runtime.api.metadata.MediaType.HTML;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_DISPOSITION;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.test.module.http.functional.matcher.HttpMessageAttributesMatchers.hasStatusCode;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiPartInputStreamParser;
import org.junit.Rule;
import org.junit.Test;

public class HttpRequestOutboundPartsTestCase extends AbstractHttpRequestTestCase {

  private static final String TEST_FILE_NAME = "auth/realm.properties";
  private static final String TEST_PART_NAME = "partName";
  public static final String PARTS = "parts";

  @Rule
  public SystemProperty sendBufferSize = new SystemProperty("sendBufferSize", "128");

  @Override
  protected String getConfigFile() {
    return "http-request-outbound-parts-config.xml";
  }

  private Collection<Part> parts;
  private List<Message> partsToSend = new LinkedList<>();
  private String requestContentType;

  @Override
  protected boolean enableHttps() {
    return true;
  }

  @Test
  public void partsAreSent() throws Exception {
    String content1 = "content 1";
    PartAttributes part1Attributes = new PartAttributes("part1", null, content1.length(), emptyMap());
    addPartToSend(content1, TEXT, part1Attributes);
    String content2 = "content 2";
    PartAttributes part2Attributes = new PartAttributes("part2", "myPart.txt", content2.length(), emptyMap());
    addPartToSend(content2.getBytes(), TEXT, part2Attributes);
    flowRunner("requestPartFlow").withPayload(getPayload()).run();

    assertThat(requestContentType, startsWith("multipart/form-data; boundary="));
    assertThat(parts.size(), equalTo(2));

    assertPart("part1", TEXT, content1);
    assertPart("part2", TEXT, content2);
    assertFormDataContentDisposition(getPart("part2"), "part2", "myPart.txt");
  }

  @Test
  public void partsCustomContentType() throws Exception {
    String content1 = "Contents 1";
    PartAttributes part1Attributes = new PartAttributes("part1", null, content1.length(), emptyMap());
    addPartToSend(content1, TEXT, part1Attributes);
    String content2 = "Contents 2";
    PartAttributes part2Attributes = new PartAttributes("part2", null, content2.length(), emptyMap());
    addPartToSend(content2, HTML, part2Attributes);

    flowRunner("requestFlow").withPayload(getPayload())
        .withMediaType(MediaType.parse("multipart/form-data2")).run();

    assertThat(requestContentType, startsWith("multipart/form-data2; boundary="));
    assertThat(parts.size(), equalTo(2));

    assertPart("part1", TEXT, content1);
    assertPart("part2", HTML, content2);
  }

  @Test
  public void filePartSetsContentDispositionWithFileName() throws Exception {
    File file = new File(IOUtils.getResourceAsUrl(TEST_FILE_NAME, getClass()).getPath());
    PartAttributes partAttributes = new PartAttributes(TEST_PART_NAME, TEST_FILE_NAME.substring(5), file.length(), emptyMap());
    addPartToSend(new FileInputStream(file), partAttributes);

    flowRunner("requestFlow").withPayload(getPayload()).run();

    Part part = getPart(TEST_PART_NAME);
    assertFormDataContentDisposition(part, TEST_PART_NAME, TEST_FILE_NAME.substring(5));
  }

  private void addPartToSend(Object content, PartAttributes partAttributes) {
    partsToSend.add(builder().payload(content).attributes(partAttributes).build());
  }

  @Test
  public void byteArrayPartSetsContentDispositionWithFileName() throws Exception {
    PartAttributes partAttributes = new PartAttributes(TEST_PART_NAME, TEST_FILE_NAME, TEST_MESSAGE.length(), emptyMap());
    addPartToSend(TEST_MESSAGE.getBytes(), TEXT, partAttributes);

    flowRunner("requestFlow").withPayload(getPayload()).run();

    Part part = getPart(TEST_PART_NAME);
    assertFormDataContentDisposition(part, TEST_PART_NAME, TEST_FILE_NAME);
  }

  @Test
  public void stringPartSetsContentDispositionWithoutFileName() throws Exception {
    addPartToSend(TEST_MESSAGE, TEXT, new PartAttributes(TEST_PART_NAME));

    flowRunner("requestFlow").withPayload(getPayload()).run();

    Part part = getPart(TEST_PART_NAME);
    assertFormDataContentDisposition(part, TEST_PART_NAME, null);
  }

  @Test
  public void sendingAttachmentBiggerThanAsyncWriteQueueSizeWorksOverHttps() throws Exception {
    // Grizzly defines the maxAsyncWriteQueueSize as 4 times the sendBufferSize
    // (org.glassfish.grizzly.nio.transport.TCPNIOConnection).
    int maxAsyncWriteQueueSize = Integer.valueOf(sendBufferSize.getValue()) * 4;
    // Set a part bigger than the queue size.
    addPartToSend(new byte[maxAsyncWriteQueueSize * 2], TEXT, new PartAttributes(TEST_PART_NAME));

    Event response = flowRunner("requestFlowTls").withPayload(TEST_MESSAGE).withFlowVariable(PARTS, partsToSend).run();

    assertThat((HttpResponseAttributes) response.getMessage().getAttributes(), hasStatusCode(OK.getStatusCode()));
  }

  private DefaultMultiPartPayload getPayload() {
    return new DefaultMultiPartPayload(partsToSend);
  }

  private void addPartToSend(Object content, MediaType contentType, PartAttributes attributes) throws Exception {
    partsToSend.add(builder().payload(content).attributes(attributes).mediaType(contentType).build());
  }

  private void assertPart(String name, MediaType expectedContentType, String expectedBody) throws Exception {
    Part part = getPart(name);
    assertThat(part, notNullValue());
    assertThat(part.getContentType(), startsWith(expectedContentType.toString()));
    assertThat(IOUtils.toString(part.getInputStream()), equalTo(expectedBody));
  }

  private void assertFormDataContentDisposition(Part part, String expectedName, String expectedFileName) {
    String expected = String.format("form-data; name=\"%s\"", expectedName);
    if (expectedFileName != null) {
      expected += String.format("; filename=\"%s\"", expectedFileName);
    }

    assertThat(part.getHeader(CONTENT_DISPOSITION), equalTo(expected));
  }

  private Part getPart(String name) {
    for (Part part : parts) {
      if (part.getName().equals(name)) {
        return part;
      }
    }
    return null;
  }

  @Override
  protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
    requestContentType = request.getHeader(CONTENT_TYPE);

    MultiPartInputStreamParser inputStreamParser =
        new MultiPartInputStreamParser(request.getInputStream(), request.getContentType(), null, null);

    try {
      parts = inputStreamParser.getParts();
    } catch (ServletException e) {
      throw new IOException(e);
    }


    response.setContentType(HTML.toString());
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().print(DEFAULT_RESPONSE);
  }
}
