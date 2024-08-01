package app

import com.google.common.base.Suppliers
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.extension.trace.propagation.JaegerPropagator
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter

object OpenTelemetryUtil {

    private val tracerSource = Suppliers.memoize { generateTracer() }

    private fun generateTracer(): Tracer {
        return GlobalOpenTelemetry.get().getTracer("HelloOtel", "0.0.1")
    }

    fun configOpenTelemetry(spanExporter: SpanExporter) {
        val jaegerPropagator: JaegerPropagator = JaegerPropagator.getInstance()
        val contextPropagators = ContextPropagators.create(jaegerPropagator)
        val spanProcessor = SimpleSpanProcessor.create(spanExporter)
        val loggingSpanExporter = SimpleSpanProcessor.create(LoggingSpanExporter.create())
        val sdkTracerProvider: SdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(spanProcessor)
                .addSpanProcessor(loggingSpanExporter)
                .build()
        val telemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider).setPropagators(contextPropagators).build()
        GlobalOpenTelemetry.set(telemetrySdk)
    }

    fun tracer(): Tracer {
        return tracerSource.get()
    }
}