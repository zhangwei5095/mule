/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.contains;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.el.context.AbstractELTestCase;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitCleaner;

import javax.activation.DataHandler;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ExpressionLanguageEnrichmentTestCase extends AbstractELTestCase {

  public ExpressionLanguageEnrichmentTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  protected MVELExpressionLanguage expressionLanguage;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() throws Exception {
    expressionLanguage = new MVELExpressionLanguage(muleContext);
    muleContext.getRegistry().registerObject(OBJECT_EXPRESSION_LANGUAGE, expressionLanguage);
  }

  @Test
  public void enrichReplacePayload() throws Exception {
    Event event = getTestEvent("foo");
    Event.Builder eventBuilder = Event.builder(event);
    expressionLanguage.enrich("message.payload", event, eventBuilder, flowConstruct, "bar");
    assertThat(eventBuilder.build().getMessage().getPayload().getValue(), is("bar"));
  }

  @Test
  public void enrichObjectPayload() throws Exception {
    Apple apple = new Apple();
    FruitCleaner fruitCleaner = new FruitCleaner() {

      @Override
      public void wash(Fruit fruit) {}

      @Override
      public void polish(Fruit fruit) {

      }
    };
    Event event = getTestEvent(apple);
    expressionLanguage.enrich("message.payload.appleCleaner", event, Event.builder(event), flowConstruct, fruitCleaner);
    assertThat(apple.getAppleCleaner(), is(fruitCleaner));
  }

  @Test
  public void enrichMessageProperty() throws Exception {
    Event event = getTestEvent("foo");
    Event.Builder eventBuilder = Event.builder(event);
    expressionLanguage.enrich("message.outboundProperties.foo", event, eventBuilder, flowConstruct, "bar");
    assertThat(eventBuilder.build().getMessage().getOutboundProperty("foo"), is("bar"));
  }

  @Test
  public void enrichMessageAttachment() throws Exception {
    DataHandler dataHandler = new DataHandler(new Object(), "test/xml");
    Event event = getTestEvent("foo");
    Event.Builder eventBuilder = Event.builder(event);
    expressionLanguage.enrich("message.outboundAttachments.foo", event, eventBuilder, flowConstruct, dataHandler);
    assertThat(eventBuilder.build().getMessage().getOutboundAttachment("foo"), is(dataHandler));
  }

  @Test
  public void enrichFlowVariable() throws Exception {
    Flow flow = new Flow("flow", muleContext);
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.builder().payload("").build()).exchangePattern(ONE_WAY).flow(flow).build();
    Event.Builder eventBuilder = Event.builder(event);
    expressionLanguage.enrich("flowVars['foo']", event, eventBuilder, flowConstruct, "bar");
    assertThat(eventBuilder.build().getVariable("foo").getValue(), is("bar"));
    assertThat(eventBuilder.build().getSession().getProperty("foo"), nullValue());
  }

  @Test
  public void enrichSessionVariable() throws Exception {
    Flow flow = new Flow("flow", muleContext);
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.builder().payload("").build()).exchangePattern(ONE_WAY).flow(flow).build();
    Event.Builder eventBuilder = Event.builder(event);
    expressionLanguage.enrich("sessionVars['foo']", event, eventBuilder, flowConstruct, "bar");
    assertThat(eventBuilder.build().getSession().getProperty("foo"), equalTo("bar"));
    assertThat(eventBuilder.build().getVariableNames(), not(contains("foo")));
  }

  @Test
  public void enrichWithDolarPlaceholder() throws Exception {
    Event event = getTestEvent("");
    Event.Builder eventBuilder = Event.builder(event);
    expressionLanguage.enrich("message.outboundProperties.put('foo', $)", event, eventBuilder, flowConstruct, "bar");
    assertThat(eventBuilder.build().getMessage().getOutboundProperty("foo"), is("bar"));
  }

}
