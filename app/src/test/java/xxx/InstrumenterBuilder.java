/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package xxx;

import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.WARNING;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.ErrorCauseExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.internal.ConfigPropertiesUtil;
import io.opentelemetry.instrumentation.api.internal.SchemaUrlProvider;
import io.opentelemetry.instrumentation.api.internal.SpanKey;
import io.opentelemetry.instrumentation.api.internal.SpanKeyProvider;

public final class InstrumenterBuilder<REQUEST, RESPONSE> {

  private static final Logger logger = Logger.getLogger(InstrumenterBuilder.class.getName());

  private static final SpanSuppressionStrategy spanSuppressionStrategy =
      SpanSuppressionStrategy.fromConfig(
          ConfigPropertiesUtil.getString(
              "otel.instrumentation.experimental.span-suppression-strategy"));

  final OpenTelemetry openTelemetry;
  final String instrumentationName;
  final SpanNameExtractor<? super REQUEST> spanNameExtractor;

  final List<AttributesExtractor<? super REQUEST, ? super RESPONSE>> attributesExtractors =
      new ArrayList<>();

  SpanKindExtractor<? super REQUEST> spanKindExtractor = SpanKindExtractor.alwaysInternal();
  ErrorCauseExtractor errorCauseExtractor = ErrorCauseExtractor.getDefault();

  InstrumenterBuilder(
      OpenTelemetry openTelemetry,
      String instrumentationName,
      SpanNameExtractor<? super REQUEST> spanNameExtractor) {
    this.openTelemetry = openTelemetry;
    this.instrumentationName = instrumentationName;
    this.spanNameExtractor = spanNameExtractor;
  }

  /**
   * Returns a new {@link Instrumenter} which will create spans with kind determined by the passed
   * {@link SpanKindExtractor} and extract context from requests with the passed {@link
   * TextMapGetter}.
   */
  // TODO: candidate for public API
  Instrumenter<REQUEST, RESPONSE> buildUpstreamInstrumenter(
      TextMapGetter<REQUEST> getter, SpanKindExtractor<REQUEST> spanKindExtractor) {
    return buildInstrumenter(
        InstrumenterConstructor.propagatingFromUpstream(requireNonNull(getter, "getter")),
        spanKindExtractor);
  }

  /**
   * Returns a new {@link Instrumenter} which will create spans with kind determined by the passed
   * {@link SpanKindExtractor} and inject context into requests with the passed {@link
   * TextMapSetter}.
   */
  // TODO: candidate for public API
  Instrumenter<REQUEST, RESPONSE> buildDownstreamInstrumenter(
      TextMapSetter<REQUEST> setter, SpanKindExtractor<REQUEST> spanKindExtractor) {
    return buildInstrumenter(
        InstrumenterConstructor.propagatingToDownstream(requireNonNull(setter, "setter")),
        spanKindExtractor);
  }

   Instrumenter<REQUEST, RESPONSE> buildInstrumenter(
      InstrumenterConstructor<REQUEST, RESPONSE> constructor,
      SpanKindExtractor<? super REQUEST> spanKindExtractor) {
    this.spanKindExtractor = spanKindExtractor;
    return constructor.create(this);
  }

  Tracer buildTracer() {
    TracerBuilder tracerBuilder =
        openTelemetry.getTracerProvider().tracerBuilder(instrumentationName);
    String schemaUrl = getSchemaUrl();
    if (schemaUrl != null) {
      tracerBuilder.setSchemaUrl(schemaUrl);
    }
    return tracerBuilder.build();
  }

  @Nullable
  private String getSchemaUrl() {
    // url set explicitly overrides url computed using attributes extractors
    Set<String> computedSchemaUrls =
        attributesExtractors.stream()
            .filter(SchemaUrlProvider.class::isInstance)
            .map(SchemaUrlProvider.class::cast)
            .flatMap(
                provider -> {
                  String url = provider.internalGetSchemaUrl();
                  return url == null ? Stream.of() : Stream.of(url);
                })
            .collect(Collectors.toSet());
    switch (computedSchemaUrls.size()) {
      case 0:
        return null;
      case 1:
        return computedSchemaUrls.iterator().next();
      default:
        logger.log(
            WARNING,
            "Multiple schemaUrls were detected: {0}. The built Instrumenter will have no schemaUrl assigned.",
            computedSchemaUrls);
        return null;
    }
  }

  SpanSuppressor buildSpanSuppressor() {
    return new SpanSuppressors.ByContextKey(
        spanSuppressionStrategy.create(getSpanKeysFromAttributesExtractors()));
  }

  private Set<SpanKey> getSpanKeysFromAttributesExtractors() {
    return attributesExtractors.stream()
        .filter(SpanKeyProvider.class::isInstance)
        .map(SpanKeyProvider.class::cast)
        .flatMap(
            provider -> {
              SpanKey spanKey = provider.internalGetSpanKey();
              return spanKey == null ? Stream.of() : Stream.of(spanKey);
            })
        .collect(Collectors.toSet());
  }

  private interface InstrumenterConstructor<RQ, RS> {
    Instrumenter<RQ, RS> create(InstrumenterBuilder<RQ, RS> builder);

    static <RQ, RS> InstrumenterConstructor<RQ, RS> internal() {
      return Instrumenter::new;
    }

    static <RQ, RS> InstrumenterConstructor<RQ, RS> propagatingToDownstream(
        TextMapSetter<RQ> setter) {
      return builder -> new PropagatingToDownstreamInstrumenter<>(builder, setter);
    }

    static <RQ, RS> InstrumenterConstructor<RQ, RS> propagatingFromUpstream(
        TextMapGetter<RQ> getter) {
      return builder -> new PropagatingFromUpstreamInstrumenter<>(builder, getter);
    }
  }

  static {
  }
}
