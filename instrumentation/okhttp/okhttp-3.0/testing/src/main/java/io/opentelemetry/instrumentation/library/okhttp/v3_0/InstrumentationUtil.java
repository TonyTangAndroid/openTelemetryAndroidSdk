/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0;


import androidx.annotation.NonNull;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class InstrumentationUtil {


    public static Span startSpan() {
        return GlobalOpenTelemetry.getTracer("TestTracer").spanBuilder("A Test Span").startSpan();
    }

    public static void setUpSpanExporter(SpanExporter spanExporter) {
        OpenTelemetrySdk openTelemetry =
                OpenTelemetrySdk.builder()
                        .setTracerProvider(getSimpleTracerProvider(spanExporter))
                        .build();
        GlobalOpenTelemetry.resetForTest();
        GlobalOpenTelemetry.set(openTelemetry);
    }


    @NonNull
    public static SdkTracerProvider getSimpleTracerProvider(SpanExporter spanExporter) {
        return SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
    }
}
