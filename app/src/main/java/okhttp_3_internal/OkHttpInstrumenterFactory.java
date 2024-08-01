/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package okhttp_3_internal;

import static io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor.alwaysClient;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractorBuilder;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class OkHttpInstrumenterFactory {

  private static final String INSTRUMENTATION_NAME = "otel_okhttp";

  public static Instrumenter<Request, Response> create(
      OpenTelemetry openTelemetry) {

    OkHttpAttributesGetter httpAttributesGetter = OkHttpAttributesGetter.INSTANCE;


    HttpSpanNameExtractorBuilder<Request> httpSpanNameExtractorBuilder =
        HttpSpanNameExtractor.builder(httpAttributesGetter);
    InstrumenterBuilder<Request, Response> builder =
        Instrumenter.<Request, Response>builder(
                openTelemetry, INSTRUMENTATION_NAME, httpSpanNameExtractorBuilder.build())
            .addOperationMetrics(HttpClientMetrics.get());
    return builder.buildInstrumenter(alwaysClient());
  }

  private OkHttpInstrumenterFactory() {}
}
