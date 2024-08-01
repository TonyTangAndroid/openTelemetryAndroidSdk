/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package okhttp_3_android;

import io.opentelemetry.api.GlobalOpenTelemetry;
import okhttp3.Interceptor;
import okhttp_3_internal.OkHttpInstrumenterFactory;
import okhttp_3_internal.TracingInterceptor;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class OkHttp3Singletons {


    public static final Interceptor TRACING_INTERCEPTOR =
            new TracingInterceptor(
                    OkHttpInstrumenterFactory.create(
                            GlobalOpenTelemetry.get()
                    ),
                    GlobalOpenTelemetry.getPropagators());

    private OkHttp3Singletons() {
    }
}
