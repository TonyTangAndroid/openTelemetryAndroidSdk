/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package mini;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import xxx.Instrumenter;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class TracingInterceptor implements Interceptor {

  private final Instrumenter<Request, Response> instrumenter;
  private final ContextPropagators propagators;

  public TracingInterceptor(
      Instrumenter<Request, Response> instrumenter) {
    this.instrumenter = instrumenter;
    this.propagators =  GlobalOpenTelemetry.getPropagators();
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    Context parentContext = Context.current();

    Context context = instrumenter.start(parentContext, request);
    request = injectContextToRequest(request, context);

    Response response = null;
    Throwable error = null;
    try (Scope ignored = context.makeCurrent()) {
      response = chain.proceed(request);
      return response;
    } catch (Exception e) {
      error = e;
      throw e;
    } finally {
      instrumenter.end(context, request, response, error);
    }
  }

  // Context injection is being handled manually for a reason: we want to use the OkHttp Request
  // type for additional AttributeExtractors provided by the user of this library
  // thus we must use Instrumenter<Request, Response>, and Request is immutable
  private Request injectContextToRequest(Request request, Context context) {
    Request.Builder requestBuilder = request.newBuilder();
    propagators
        .getTextMapPropagator()
        .inject(context, requestBuilder, RequestHeaderSetter.INSTANCE);
    return requestBuilder.build();
  }
}
