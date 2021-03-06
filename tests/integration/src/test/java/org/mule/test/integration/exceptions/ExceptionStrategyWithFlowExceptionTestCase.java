/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.message.ExceptionMessage;

import org.junit.Test;

public class ExceptionStrategyWithFlowExceptionTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-with-flow-exception.xml";
  }

  @Test
  public void testFlowExceptionExceptionStrategy() throws Exception {
    flowRunner("customException").withPayload(getTestMuleMessage(TEST_MESSAGE)).asynchronously().run();
    MuleClient client = muleContext.getClient();
    InternalMessage message = client.request("test://out", RECEIVE_TIMEOUT).getRight().get();

    assertNotNull("request returned no message", message);
    assertTrue(message.getPayload().getValue() instanceof ExceptionMessage);
  }

  public static class ExceptionThrower implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      throw new MessagingException(event, null);
    }
  }
}
