/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package mini;

import static java.util.Collections.singletonList;

import java.util.function.Supplier;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.library.okhttp.v3_0.OkHttpInstrumentationConfig;
import io.opentelemetry.instrumentation.library.okhttp.v3_0.internal.CachedSupplier;
import io.opentelemetry.instrumentation.library.okhttp.v3_0.internal.LazyInterceptor;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpAttributesGetter;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpInstrumenterFactory;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.TracingInterceptor;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

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
                                    singletonList(
                                            PeerServiceAttributesExtractor.create(
                                                    OkHttpAttributesGetter.INSTANCE,
                                                    OkHttpInstrumentationConfig
                                                            .newPeerServiceResolver())),
                                    OkHttpInstrumentationConfig
                                            .emitExperimentalHttpClientMetrics()));

    public static final Interceptor TRACING_INTERCEPTOR =
            new LazyInterceptor<>(
                    CachedSupplier.create(
                            () ->
                                    new TracingInterceptor(
                                            INSTRUMENTER.get(),
                                            GlobalOpenTelemetry.getPropagators())));

    private OkHttp3Singletons() {
    }
}
