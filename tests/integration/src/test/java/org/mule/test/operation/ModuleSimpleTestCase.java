/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.operation;

import static org.hamcrest.MatcherAssert.assertThat;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.Event;

import org.hamcrest.core.Is;
import org.junit.Ignore;
import org.junit.Test;

public class ModuleSimpleTestCase extends FunctionalTestCase {

  @org.junit.Rule
  public org.junit.rules.Timeout globalTimeout = new org.junit.rules.Timeout(3000000);

  @Override
  protected String getConfigFile() {
    return "module/flows-using-module-simple.xml";
  }

  @Test
  public void testSetPayloadHardcodedFlow() throws Exception {
    Event event = flowRunner("testSetPayloadHardcodedFlow").run();
    assertThat(event.getMessage().getPayload().getValue(), Is.is("hardcoded value"));
  }

  @Test
  public void testSetPayloadParamFlow() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadParamFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("new payload"));
  }

  @Test
  public void testSetPayloadParamDefaultFlow() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadParamDefaultFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is(15));
  }

  @Test
  @Ignore //until we have muleevent/flowVars isolation this test will fail
  //TODO talk to PLG this one should not be ignored.. and this is the reason why we need a custom MP that does the chain, so that we can manipulate the MuleEvent to provide scopinggg
  public void testSetPayloadNoSideEffectFlow() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadNoSideEffectFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("10"));
    assertThat(muleEvent.getVariable("testVar"), Is.is("unchanged value"));
  }

  @Test
  public void testDoNothingFlow() throws Exception {
    Event muleEvent = flowRunner("testDoNothingFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("before calling"));
    assertThat(muleEvent.getVariable("variableBeforeCalling").getValue(), Is.is("value of flowvar before calling"));
  }

  @Test
  public void testSetPayloadParamValueAppender() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadParamValueAppender").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("new payload from module"));
  }

  @Test
  public void testSetPayloadAddParamsValues() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadAddParamsValues").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is(15));

  }

  //@Test
  //@Ignore
  //<!-- TODO WIP-OPERATIONS this won't be acceptable in the XML as the XSD will check for it before running the app -->
  //public void testWithoutParametersFlow() throws Exception
  //{
  //    try{
  //        flowRunner("testWithoutParametersFlow").run();
  //        fail("should not have reach this point");
  //    }catch (MessagingException me){
  //        assertThat(me.getCause(),instanceOf(ExpressionRuntimeException.class));
  //        assertThat(me.getCause().getMessage(), Is.is("Execution of the expression \"value\" failed.") );
  //    }
  //}
  //
  //@Test
  //@Ignore
  //<!-- TODO WIP-OPERATIONS this won't be acceptable in the XML as the XSD will check for it before running the app -->
  //public void testWithMoreThanExpectedParametersFlow() throws Exception
  //{
  //    try{
  //        flowRunner("testWithMoreThanExpectedParametersFlow").run();
  //        fail("should not have reach this point");
  //    }catch (MessagingException me){
  //        assertThat(me.getCause(),instanceOf(ExpressionRuntimeException.class));
  //        assertThat(me.getCause().getMessage(), Is.is("Execution of the expression \"value\" failed.") );
  //    }
  //}
}
