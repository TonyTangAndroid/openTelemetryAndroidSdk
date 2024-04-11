/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.instrumentation.library.okhttp.v3_0

import com.google.common.truth.Truth.assertThat
import com.google.gson.annotations.SerializedName
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.extension.trace.propagation.JaegerPropagator
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.http.GET
import retrofit2.http.Header
import java.io.IOException


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class JaegerPropagatorMiniTest {
    private val server = MockWebServer()
    private val restApi by lazy { RestApiUtil.restApi(server) }
    private val inMemorySpanExporter: InMemorySpanExporter = InMemorySpanExporter.create()

    @Before
    fun setup() {
        //arrange
        server.start()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"token":"1234"}
        """.trimIndent()))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"status":"granted"}
        """.trimIndent()))
        GlobalOpenTelemetry.resetForTest()
        configOpenTelemetry()

    }

    @After
    fun teardown() {
        //clean up
        server.shutdown()
        inMemorySpanExporter.reset()
        GlobalOpenTelemetry.resetForTest()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun `case 1  when jaeger propagator is added it will trigger the request with uber header`() {
        val tracer: Tracer = GlobalOpenTelemetry.getTracer("TestTracer")
        Context.current().with(rootBaggage()).makeCurrent().use {
            val rootSpan: Span = triggerRootSpan(tracer, restApi)
            assertRoot(rootSpan)
            Context.current().with(loggedInBaggage()).makeCurrent().use {
                val loggedInSpan: Span = triggerLoggedInSpan(tracer, restApi)
                assertLoggedIn(inMemorySpanExporter, server, loggedInSpan)
            }
        }
    }

    private fun configOpenTelemetry() {
        //Make `uber-trace-id` attached.
        val jaegerPropagator: JaegerPropagator = JaegerPropagator.getInstance()
        val contextPropagators = ContextPropagators.create(jaegerPropagator)
        val spanProcessor = SimpleSpanProcessor.create(inMemorySpanExporter)
        val tracer = SdkTracerProvider.builder().addSpanProcessor(spanProcessor).build()
        val telemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(tracer)
                .setPropagators(contextPropagators)
                .build()
        GlobalOpenTelemetry.set(telemetrySdk)
    }


    private fun triggerRootSpan(tracer: Tracer, restApi: RestApi): Span {
        val rootSpan: Span = rootSpan(tracer)
        rootSpan.addEvent("start_logging_in")
        //act
        rootSpan.makeCurrent().use {
            login(restApi)
        }
        rootSpan.addEvent("finished_logging_in")
        rootSpan.end()
        return rootSpan
    }


    private fun triggerLoggedInSpan(tracer: Tracer, restApi: RestApi): Span {
        val loggedInSpan: Span = rootSpan(tracer)
        loggedInSpan.addEvent("start_fetching_profile")
        //act
        loggedInSpan.makeCurrent().use {
            checkProfile(restApi)
        }
        loggedInSpan.addEvent("end_fetching_profile")
        loggedInSpan.end()
        return loggedInSpan
    }

    private fun rootSpan(tracer: Tracer): Span {
        val spanBuilder: SpanBuilder = tracer.spanBuilder("A Test Span")
        spanBuilder.setAttribute("root_key_1", "root_key_2")
        spanBuilder.setSpanKind(SpanKind.CLIENT)
        return spanBuilder.startSpan()
    }


    /**
     *Per the following assertion statement, here is what I take away:
     *
     * 0, All start from `GlobalOpenTelemetry.getTracer("TestTracer")`. It establishes a Tracer with unique trace id.
     * 1, Once a tracer is active, it could trace different type actions. For here, we are using `OkHttp3Singletons.TRACING_INTERCEPTOR` to trace the network data out of box.
     * 2, As we registered `JaegerPropagator`, hence we could get recordedRequest.headers["uber-trace-id"] out of box.
     * 3, As we have `InMemorySpanExporter`, hence we could get the span data from `InMemorySpanExporter`.
     * 4, Question so far: How could we associate the baggage with the tracing? By explicitly call ` Context.current()`?
     */
    private fun assertRoot(rootSpan: Span) {
        val recordedRequest = server.takeRequest()
        //affirm
        assertThat(recordedRequest.headers).hasSize(8)
        val list: List<Pair<String, String>> = recordedRequest.headers.filter { it.first.startsWith("uberctx") }
        assertThat(list).containsExactlyElementsIn(
                listOf(Pair("uberctx-user.id", "321"), Pair("uberctx-user.name", "jack"))
        )
        val uberTraceId = recordedRequest.headers["uber-trace-id"]
        val spanTraceId = rootSpan.spanContext.traceId
        //example value 8d828d3c7c8663418b067492675bef12
        assertThat(spanTraceId).isNotEmpty()
        //example value  8d828d3c7c8663418b067492675bef12:dae708107c50eb0f:0:1
        assertThat(uberTraceId).isNotEmpty()
        assertThat(uberTraceId).startsWith(spanTraceId)
        assertThat(uberTraceId).isNotEqualTo("8d828d3c7c8663418b067492675bef12")
        assertThat(uberTraceId).isEqualTo(assembleRawTraceId(inMemorySpanExporter.finishedSpanItems[0]))

    }


    private fun assertLoggedIn(inMemorySpanExporter: InMemorySpanExporter, server: MockWebServer, rootSpan: Span) {
        val finishedSpanItems = inMemorySpanExporter.finishedSpanItems
        assertThat(finishedSpanItems).hasSize(4)
        val recordedRequest = server.takeRequest()
        //affirm
        assertThat(recordedRequest.headers).hasSize(7)
        val list: List<Pair<String, String>> = recordedRequest.headers.filter { it.first.startsWith("uberctx") }
        assertThat(list).containsExactlyElementsIn(
                listOf(Pair("uberctx-user.logged_in", "true"))
        )
        val uberTraceId = recordedRequest.headers["uber-trace-id"]
        val spanTraceId = rootSpan.spanContext.traceId
        //example value 8d828d3c7c8663418b067492675bef12
        assertThat(spanTraceId).isNotEmpty()
        //example value  8d828d3c7c8663418b067492675bef12:dae708107c50eb0f:0:1
        assertThat(uberTraceId).isNotEmpty()
        assertThat(uberTraceId).startsWith(spanTraceId)
        assertThat(uberTraceId).isNotEqualTo("8d828d3c7c8663418b067492675bef12")
        val spanData: SpanData = finishedSpanItems[2]
        val assembledTracedId = assembleRawTraceId(spanData)
        assertThat(uberTraceId).isEqualTo(assembledTracedId)
        assertThat(finishedSpanItems[2].spanId).isNotEqualTo(rootSpan.spanContext.spanId)
        assertThat(finishedSpanItems[3].spanId).isEqualTo(rootSpan.spanContext.spanId)
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

    private fun login(restApi: RestApi): UserToken {
        return restApi.login(1).execute().body()!!
    }

    private fun checkProfile(restApi: RestApi): UserStatus {
        return restApi.profile("1234").execute().body()!!
    }

    private fun rootBaggage(): Baggage {
        return Baggage.builder()
                .put("user.name", "jack")
                .put("user.id", "321")
                .build()
    }

    private fun loggedInBaggage(): Baggage {
        return Baggage.builder()
                .put("user.logged_in", "true")
                .build()
    }
}

interface RestApi {
    @GET("auth")
    fun login(@Header("x-bypass") flag: Int): retrofit2.Call<UserToken>

    @GET("profile")
    fun profile(@Header("token") flag: String): retrofit2.Call<UserStatus>
}

data class UserToken(@SerializedName("token") val token: String)
data class UserStatus(@SerializedName("status") val status: String)
