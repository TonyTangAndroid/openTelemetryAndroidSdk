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
    private val spanExporter: InMemorySpanExporter = InMemorySpanExporter.create()

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
        spanExporter.reset()
        GlobalOpenTelemetry.resetForTest()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun `case 1  when jaeger propagator is added it will trigger the request with uber header`() {
        val tracer: Tracer = GlobalOpenTelemetry.getTracer("TestTracer")
        Context.current().with(rootBaggage()).makeCurrent().use {
            assertRoot(triggerRootSpan(tracer, restApi))
            Context.current().with(loggedInBaggage()).makeCurrent().use {
                assertLoggedIn( triggerLoggedInSpan(tracer, restApi))
            }
        }
    }

    private fun configOpenTelemetry() {
        //Make `uber-trace-id` attached.
        val jaegerPropagator: JaegerPropagator = JaegerPropagator.getInstance()
        val contextPropagators = ContextPropagators.create(jaegerPropagator)
        val spanProcessor = SimpleSpanProcessor.create(spanExporter)
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


    private fun assertRoot(rootSpan: Span) {
        val request = server.takeRequest()
        //affirm
        assertThat(request.headers).hasSize(8)
        val list: List<Pair<String, String>> = request.headers.filter { it.first.startsWith("uberctx") }
        assertThat(list).containsExactlyElementsIn(
                listOf(Pair("uberctx-user.id", "321"), Pair("uberctx-user.name", "jack"))
        )
        //example value 8d828d3c7c8663418b067492675bef12
        assertThat(rootSpan.spanContext.traceId).isNotEmpty()
        //example value  8d828d3c7c8663418b067492675bef12:dae708107c50eb0f:0:1
        assertThat(request.headers["uber-trace-id"]).isNotEmpty()
        assertThat(request.headers["uber-trace-id"]).startsWith(rootSpan.spanContext.traceId)
        assertThat(request.headers["uber-trace-id"]).isNotEqualTo("8d828d3c7c8663418b067492675bef12")
        assertThat(request.headers["uber-trace-id"]).isEqualTo(assembleRawTraceId(spanExporter.finishedSpanItems[0]))

    }


    private fun assertLoggedIn( rootSpan: Span) {
        assertThat(spanExporter.finishedSpanItems).hasSize(4)
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
        val assembledTracedId = assembleRawTraceId(spanExporter.finishedSpanItems[2])
        assertThat(uberTraceId).isEqualTo(assembledTracedId)

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
