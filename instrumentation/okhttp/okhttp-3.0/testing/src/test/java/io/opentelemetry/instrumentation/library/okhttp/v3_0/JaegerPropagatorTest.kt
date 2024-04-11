/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.instrumentation.library.okhttp.v3_0

import com.google.common.truth.Truth.assertThat
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.extension.trace.propagation.JaegerPropagator
import io.opentelemetry.instrumentation.library.okhttp.v3_0.internal.OkHttp3Singletons
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.addHeaderLenient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class JaegerPropagatorTest {


    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun `case 1  when jaeger propagator is added it will trigger the request with uber header`() {
        //arrange
        val server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setResponseCode(200))
        GlobalOpenTelemetry.resetForTest()


        //step 1: config the telemetrySdk
        val inMemorySpanExporter = InMemorySpanExporter.create()
        val jaegerPropagator: JaegerPropagator = JaegerPropagator.getInstance()
        val spanProcessor: SpanProcessor = SimpleSpanProcessor.create(inMemorySpanExporter)
        val sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(spanProcessor)
                .build()
        //Make `uber-trace-id` attached.
        val contextPropagators = ContextPropagators.create(jaegerPropagator)
        val telemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
                .setPropagators(contextPropagators)
                .build()
        GlobalOpenTelemetry.set(telemetrySdk)

        //step 2: start trace
        val tracer: Tracer = GlobalOpenTelemetry.getTracer("TestTracer")
        val spanBuilder: SpanBuilder = tracer.spanBuilder("A Test Span")
        val baggage = Baggage.builder()
                .put("user.name", "jack")
                .put("user.id", "123")
                .build()
        val makeCurrent: Scope = Context.current().with(baggage).makeCurrent()
        makeCurrent.use {
            spanBuilder.setAttribute("root_key_1", "root_key_2")
            spanBuilder.setSpanKind(SpanKind.CLIENT)
            val rootSpan = spanBuilder.startSpan()
            rootSpan.addEvent("started_event")

            //act
            rootSpan.makeCurrent().use {
                execute(rootSpan, server)
            }
            rootSpan.addEvent("ended_event")
            rootSpan.end()

            //assert
            assert(inMemorySpanExporter, server, rootSpan)
        }

        //clean up
        server.shutdown()
        inMemorySpanExporter.reset()
    }

    /**
     *Per the following assertion statement, here is what I take away:
     *
     * 0, All start from `GlobalOpenTelemetry.getTracer("TestTracer")`. It establishes a Tracer with unique trace id.
     * 1, Once a tracer is active, it could trace different type actions. For here, we are using `OkHttp3Singletons.TRACING_INTERCEPTOR` to trace the network data out of box.
     * 2, As we registered `JaegerPropagator`, hence we could get recordedRequest.headers["uber-trace-id"] out of box.
     * 3, As we have `InMemorySpanExporter`, hence we could get the span data from `InMemorySpanExporter`.
     * 4, Question so far: How could we associate the baggage with the tracing? By explicitly call ` Context.current()`?
     *
     *     user.id: 123
     *     user.name: jack
     *     fixed_header_key: fixed_header_value
     *     Host: localhost:49792
     *     Connection: Keep-Alive
     *     Accept-Encoding: gzip
     *     User-Agent: okhttp/4.12.0
     *     uber-trace-id: 9ff9ce0bc81902bdec474411df38a506:c20f4deeb034d9b5:0:1
     *     uberctx-abc: 456
     *
     *     user.id: 123
     *     user.name: jack
     *     fixed_header_key: fixed_header_value
     *     Host: localhost:49826
     *     Connection: Keep-Alive
     *     Accept-Encoding: gzip
     *     User-Agent: okhttp/4.12.0
     *     uber-trace-id: 1d8de29f65d4bb60a65c344ab1470210:cb859dc8a9290eee:0:1
     *     uberctx-user.id: 123
     *     uberctx-user.name: jack
     */
    private fun assert(inMemorySpanExporter: InMemorySpanExporter, server: MockWebServer, rootSpan: Span) {
        val finishedSpanItems = inMemorySpanExporter.finishedSpanItems
        assertThat(finishedSpanItems).hasSize(2)
        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.headers).hasSize(8)
        val uberTraceId = recordedRequest.headers["uber-trace-id"]
        val uberBaggage = recordedRequest.headers["uberctx-abc"]
        assertThat(uberBaggage).isEqualTo("456")

        val spanTraceId = rootSpan.spanContext.traceId
        //example value 8d828d3c7c8663418b067492675bef12
        assertThat(spanTraceId).isNotEmpty()
        //example value  8d828d3c7c8663418b067492675bef12:dae708107c50eb0f:0:1
        assertThat(uberTraceId).isNotEmpty()
        assertThat(uberTraceId).startsWith(spanTraceId)
        assertThat(uberTraceId).isNotEqualTo("8d828d3c7c8663418b067492675bef12")
        val spanData: SpanData = finishedSpanItems[0]
        val assembledTracedId = assembleRawTraceId(spanData)
        assertThat(uberTraceId).isEqualTo(assembledTracedId)
        assertThat(finishedSpanItems[0].spanId).isNotEqualTo(rootSpan.spanContext.spanId)
        assertThat(finishedSpanItems[1].spanId).isEqualTo(rootSpan.spanContext.spanId)
        assertThat(spanData.attributes[AttributeKey.longKey("http.response.status_code")]).isEqualTo(200)
        assertThat(spanData.attributes[AttributeKey.stringKey("http.response.status_code")]).isNull()

        val currentContext = Context.current()
        assertThat(currentContext).isNotNull()

    }


    private fun assembleRawTraceId(spanData: SpanData): String {
        val traceId = spanData.traceId
        val spanId = spanData.spanId
        return "$traceId:$spanId:0:1"
    }

    private fun execute(parentSpan: Span, server: MockWebServer) {
        val client: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor(TestInjectingIntercepting())
//                .addInterceptor(OpenTelemetryBaggageInterceptorx())
                //Pay attention that this is done without any context related to open telemetry.
                .addNetworkInterceptor(OkHttp3Singletons.TRACING_INTERCEPTOR)
                .addInterceptor {
                    response(parentSpan, it)
                }
                .build()
        createCall(client, server).execute().close()
    }

    private fun response(parentSpan: Span, chain: Interceptor.Chain): Response {
        val currentSpanContext: SpanContext = Span.current().spanContext
        assertThat(currentSpanContext.traceId).isEqualTo(parentSpan.spanContext.traceId)
        return chain.proceed(chain.request())
    }

    private fun createCall(client: OkHttpClient, server: MockWebServer): Call {
        val request: Request = Request.Builder().url(server.url("/test/")).build()
        return client.newCall(request)
    }


}
