/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;
import org.mockito.verification.VerificationMode;

public class TypeSafeExpressionValueResolverTestCase extends AbstractMuleContextTestCase {

  private static final String HELLO_WORLD = "Hello World!";

  private MVELExpressionLanguage expressionLanguage;

  @Override
  protected void doSetUp() throws Exception {
    muleContext = spy(muleContext);
    expressionLanguage = spy((MVELExpressionLanguage) muleContext.getExpressionLanguage());

    when(muleContext.getExpressionLanguage()).thenReturn(expressionLanguage);
  }

  @Test
  public void expressionLanguageWithoutTransformation() throws Exception {
    assertResolved(getResolver("#['Hello ' + payload]", String.class).resolve(getTestEvent("World!")), HELLO_WORLD, never());
  }

  @Test
  public void expressionTemplateWithoutTransformation() throws Exception {
    assertResolved(getResolver("Hello #[payload]", String.class).resolve(getTestEvent("World!")), HELLO_WORLD, times(1));
  }

  @Test
  public void constant() throws Exception {
    assertResolved(getResolver("Hello World!", String.class).resolve(getTestEvent(HELLO_WORLD)), HELLO_WORLD, never());
  }

  @Test
  public void expressionWithTransformation() throws Exception {
    assertResolved(getResolver("#[true]", String.class).resolve(getTestEvent(HELLO_WORLD)), "true", never());
  }

  @Test
  public void templateWithTransformation() throws Exception {
    assertResolved(getResolver("tru#['e']", String.class).resolve(getTestEvent(HELLO_WORLD)), "true", times(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullExpression() throws Exception {
    getResolver(null, String.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void blankExpression() throws Exception {
    getResolver(EMPTY, String.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullExpectedType() throws Exception {
    getResolver("#[payload]", null);
  }


  private void assertResolved(Object resolvedValue, Object expected, VerificationMode expressionManagerVerificationMode) {
    assertThat(resolvedValue, instanceOf(String.class));
    assertThat(resolvedValue, equalTo(expected));
    verifyExpressionManager(expressionManagerVerificationMode);
  }

  private void verifyExpressionManager(VerificationMode mode) {
    verify(expressionLanguage, mode).parse(anyString(), any(Event.class), any(FlowConstruct.class));
  }

  private ValueResolver getResolver(String expression, Class<?> expectedType) throws Exception {
    return new TypeSafeExpressionValueResolver(expression, expectedType, muleContext);
  }
}
