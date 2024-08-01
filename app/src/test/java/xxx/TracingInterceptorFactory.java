/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package xxx;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import mini.TracingInterceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class TracingInterceptorFactory {


    public static TracingInterceptor getTracingInterceptor() {
        Instrumenter<Request, Response> instrumenter = createInstrument();
        return new TracingInterceptor(instrumenter);
    }


    public static Instrumenter<Request, Response> createInstrument() {
        return Instrumenter.<Request, Response>builder(GlobalOpenTelemetry.get(), "xxx", Request::toString).buildInstrumenter(request -> client());
    }

    private static SpanKind client() {
        return SpanKind.CLIENT;
    }

}
