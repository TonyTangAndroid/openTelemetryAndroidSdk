/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package network;

import io.opentelemetry.api.GlobalOpenTelemetry;
import okhttp3.Interceptor;
import okhttp_3_internal.OkHttpInstrumenterFactory;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class OtelTracingInterceptorUtil {


    public static final Interceptor TRACING_INTERCEPTOR =
            new OtelTracingInterceptor(
                    OkHttpInstrumenterFactory.create(
                            GlobalOpenTelemetry.get()
                    ),
                    GlobalOpenTelemetry.getPropagators());

    private OtelTracingInterceptorUtil() {
    }
}
