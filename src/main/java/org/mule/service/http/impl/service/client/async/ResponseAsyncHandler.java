/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.impl.service.client.async;

import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.impl.service.client.HttpResponseCreator;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Non blocking {@link com.ning.http.client.AsyncHandler} which waits to load the whole response to memory before propagating it.
 *
 * @since 1.0
 */
public class ResponseAsyncHandler extends AsyncCompletionHandler<Response> {

  private static final Logger logger = LoggerFactory.getLogger(ResponseAsyncHandler.class);

  private final CompletableFuture<HttpResponse> future;
  private final HttpResponseCreator httpResponseCreator = new HttpResponseCreator();

  public ResponseAsyncHandler(CompletableFuture<HttpResponse> future) {
    this.future = future;
  }

  @Override
  public Response onCompleted(Response response) throws Exception {
    try {
      future.complete(httpResponseCreator.create(response, response.getResponseBodyAsStream()));
    } catch (Throwable t) {
      onThrowable(t);
    }
    return null;
  }

  @Override
  public void onThrowable(Throwable t) {
    logger.debug("Error handling HTTP response.", t);
    Exception exception;
    if (t instanceof TimeoutException) {
      exception = (TimeoutException) t;
    } else if (t instanceof IOException) {
      exception = (IOException) t;
    } else {
      exception = new IOException(t.getMessage(), t);
    }
    future.completeExceptionally(exception);
  }

}
