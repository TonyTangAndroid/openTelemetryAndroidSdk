/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package okhttp_3_android;

import static java.util.Collections.emptyList;

import java.util.function.Supplier;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp_3_config.OkHttpInstrumentationConfig;
import okhttp_3_internal.OkHttpInstrumenterFactory;
import okhttp_3_internal.TracingInterceptor;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class OkHttp3Singletons {

    private static final Supplier<Instrumenter<Request, Response>> INSTRUMENTER =
            CachedSupplier.create(
                    () ->
                            OkHttpInstrumenterFactory.create(
                                    GlobalOpenTelemetry.get(),
                                    builder ->
                                            builder.setCapturedRequestHeaders(
                                                            OkHttpInstrumentationConfig
                                                                    .getCapturedRequestHeaders())
                                                    .setCapturedResponseHeaders(
                                                            OkHttpInstrumentationConfig
                                                                    .getCapturedResponseHeaders())
                                                    .setKnownMethods(
                                                            OkHttpInstrumentationConfig
                                                                    .getKnownMethods()),
                                    spanNameExtractorConfigurer ->
                                            spanNameExtractorConfigurer.setKnownMethods(
                                                    OkHttpInstrumentationConfig.getKnownMethods()),
                                    emptyList(),
                                    OkHttpInstrumentationConfig
                                            .emitExperimentalHttpClientMetrics()));

    public static final Interceptor TRACING_INTERCEPTOR =
            new LazyInterceptor<>(
                    CachedSupplier.create(
                            () ->
                                    new TracingInterceptor(
                                            INSTRUMENTER.get(),
                                            GlobalOpenTelemetry.getPropagators())));

    private OkHttp3Singletons() {}
}
