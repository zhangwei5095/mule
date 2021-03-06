/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.pop3;

import static org.mule.extension.email.internal.EmailProtocol.POP3;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.POP3_PORT;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.CONNECTION;

import org.mule.extension.email.internal.mailbox.AbstractMailboxConnectionProvider;
import org.mule.extension.email.internal.mailbox.MailboxConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * A {@link ConnectionProvider} that returns instances of pop3 based {@link MailboxConnection}s.
 *
 * @since 4.0
 */
@Alias("pop3")
@DisplayName("POP3 Connection")
public class POP3Provider extends AbstractMailboxConnectionProvider<MailboxConnection> {

  /**
   * The port number of the mail server. '110' by default.
   */
  @Parameter
  @Optional(defaultValue = POP3_PORT)
  @Placement(group = CONNECTION, order = 2)
  private String port;

  /**
   * {@inheritDoc}
   */
  @Override
  public MailboxConnection connect() throws ConnectionException {
    return new MailboxConnection(POP3, settings.getUser(), settings.getPassword(), settings.getHost(), port,
                                 getConnectionTimeout(), getReadTimeout(), getWriteTimeout(), getProperties());
  }
}
