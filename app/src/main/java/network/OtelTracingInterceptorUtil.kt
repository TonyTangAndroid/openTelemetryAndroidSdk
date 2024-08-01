package network

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientMetrics
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor
import okhttp3.Request
import okhttp3.Response
import okhttp_3_internal.OkHttpAttributesGetter

object OtelTracingInterceptorUtil {
    private const val INSTRUMENTATION_NAME = "otel_okhttp"

    fun create(
        openTelemetry: OpenTelemetry
    ): Instrumenter<Request, Response> {
        val httpAttributesGetter = OkHttpAttributesGetter.INSTANCE
        val httpSpanNameExtractorBuilder =
            HttpSpanNameExtractor.builder(httpAttributesGetter)
        val builder =
            Instrumenter.builder<Request, Response>(
                openTelemetry, INSTRUMENTATION_NAME, httpSpanNameExtractorBuilder.build()
            )
                .addOperationMetrics(HttpClientMetrics.get())
        return builder.buildInstrumenter(SpanKindExtractor.alwaysClient())
    }

    fun tracingInterceptor(): OtelTracingInterceptor {
        return OtelTracingInterceptor(
            create(
                GlobalOpenTelemetry.get()
            ),
            GlobalOpenTelemetry.getPropagators()
        )
    }
}
