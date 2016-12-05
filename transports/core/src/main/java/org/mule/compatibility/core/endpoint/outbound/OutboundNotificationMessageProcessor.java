/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.outbound;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.context.notification.EndpointMessageNotification;
import org.mule.compatibility.core.transport.AbstractConnector;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.ObjectUtils;

/**
 * Publishes a {@link EndpointMessageNotification}'s when a message is sent or dispatched.
 */

public class OutboundNotificationMessageProcessor extends AbstractAnnotatedObject implements Processor {

  private OutboundEndpoint endpoint;

  public OutboundNotificationMessageProcessor(OutboundEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public Event process(Event event) throws MuleException {
    AbstractConnector connector = (AbstractConnector) endpoint.getConnector();
    if (connector.isEnableMessageEvents(endpoint.getMuleContext())) {
      int notificationAction;
      if (endpoint.getExchangePattern().hasResponse()) {
        notificationAction = EndpointMessageNotification.MESSAGE_SEND_END;
      } else {
        notificationAction = EndpointMessageNotification.MESSAGE_DISPATCH_END;
      }
      dispatchNotification(new EndpointMessageNotification(event.getMessage(), endpoint, endpoint.getFlowConstruct(),
                                                           notificationAction));
    }

    return event;
  }

  public void dispatchNotification(EndpointMessageNotification notification) {
    AbstractConnector connector = (AbstractConnector) endpoint.getConnector();
    if (notification != null && connector.isEnableMessageEvents(endpoint.getMuleContext())) {
      connector.fireNotification(notification, endpoint.getMuleContext());
    }
  }

  public EndpointMessageNotification createBeginNotification(Event event) {
    AbstractConnector connector = (AbstractConnector) endpoint.getConnector();
    if (connector.isEnableMessageEvents(endpoint.getMuleContext())) {
      int notificationAction;
      if (endpoint.getExchangePattern().hasResponse()) {
        notificationAction = EndpointMessageNotification.MESSAGE_SEND_BEGIN;
      } else {
        notificationAction = EndpointMessageNotification.MESSAGE_DISPATCH_BEGIN;
      }
      return new EndpointMessageNotification(event.getMessage(), endpoint, endpoint.getFlowConstruct(), notificationAction);
    }

    return null;
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }
}
