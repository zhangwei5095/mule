/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CopyPropertiesProcessorTestCase extends AbstractMuleTestCase {

  public static final Charset ENCODING = US_ASCII;
  public static final String INBOUND_PROPERTY_KEY = "propKey";
  public static final DataType PROPERTY_DATA_TYPE = DataType.STRING;
  private static final Serializable PROPERTY_VALUE = "propValue";

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleContext mockMuleContext;

  private FlowConstruct flow;

  private InternalMessage muleMessage;

  @Mock
  private ExpressionLanguage mockExpressionLanguage;

  @Before
  public void setUp() throws Exception {
    when(mockMuleContext.getExpressionLanguage()).thenReturn(mockExpressionLanguage);
    when(mockExpressionLanguage.parse(anyString(), any(Event.class), any(FlowConstruct.class)))
        .thenAnswer(invocation -> (String) invocation.getArguments()[0]);

    muleMessage = InternalMessage.builder().payload("").mediaType(PROPERTY_DATA_TYPE.getMediaType()).build();
    flow = mock(FlowConstruct.class);
    when(flow.getMuleContext()).thenReturn(mockMuleContext);
  }

  @Test
  public void testCopySingleProperty() throws MuleException {
    CopyPropertiesProcessor copyPropertiesTransformer = createCopyPropertiesTransformer(INBOUND_PROPERTY_KEY);
    muleMessage =
        InternalMessage.builder(muleMessage).addInboundProperty(INBOUND_PROPERTY_KEY, PROPERTY_VALUE, PROPERTY_DATA_TYPE).build();
    Event muleEvent = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR)).message(muleMessage).build();
    final InternalMessage transformed = copyPropertiesTransformer.process(muleEvent).getMessage();

    assertThat(transformed.getOutboundProperty(INBOUND_PROPERTY_KEY), is(PROPERTY_VALUE));
    assertThat(transformed.getInboundPropertyNames(), hasSize(1));
  }

  @Test
  public void testCopyNonExistentProperty() throws MuleException {
    CopyPropertiesProcessor copyPropertiesTransformer = createCopyPropertiesTransformer(INBOUND_PROPERTY_KEY);
    Event muleEvent = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR)).message(muleMessage).build();
    final InternalMessage transformed = copyPropertiesTransformer.process(muleEvent).getMessage();

    assertThat(transformed.getInboundPropertyNames(), hasSize(0));
  }

  @Test
  public void testCopyUsingRegex() throws MuleException {
    CopyPropertiesProcessor copyPropertiesTransformer = createCopyPropertiesTransformer("MULE_*");

    muleMessage = InternalMessage.builder(muleMessage).addInboundProperty("MULE_ID", PROPERTY_VALUE, PROPERTY_DATA_TYPE)
        .addInboundProperty("MULE_GROUP_ID", PROPERTY_VALUE, PROPERTY_DATA_TYPE)
        .addInboundProperty("SomeVar", PROPERTY_VALUE, PROPERTY_DATA_TYPE).build();
    Event muleEvent = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR)).message(muleMessage).build();
    final InternalMessage transformed = copyPropertiesTransformer.process(muleEvent).getMessage();

    assertThat(transformed.getOutboundProperty("SomeVar"), is(nullValue()));
    assertThat(transformed.getOutboundProperty("MULE_ID"), is(PROPERTY_VALUE));
    assertThat(transformed.getOutboundProperty("MULE_GROUP_ID"), is(PROPERTY_VALUE));
  }

  public CopyPropertiesProcessor createCopyPropertiesTransformer(String inboundPropertyKey) throws InitialisationException {
    CopyPropertiesProcessor copyPropertiesTransformer = new CopyPropertiesProcessor();
    copyPropertiesTransformer.setMuleContext(mockMuleContext);
    copyPropertiesTransformer.setPropertyName(inboundPropertyKey);
    copyPropertiesTransformer.initialise();

    return copyPropertiesTransformer;
  }
}
