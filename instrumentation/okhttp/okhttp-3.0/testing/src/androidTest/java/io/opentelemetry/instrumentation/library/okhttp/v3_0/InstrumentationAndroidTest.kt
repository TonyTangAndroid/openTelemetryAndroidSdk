/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.instrumentation.library.okhttp.v3_0

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.data.SpanData
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.IOException

class InstrumentationAndroidTest {
    private lateinit var server: MockWebServer

    @Before
    @Throws(IOException::class)
    fun setUp() {
        server = MockWebServer()
        server.start()
        GlobalOpenTelemetryUtil.setUpSpanExporter(inMemorySpanExporter)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun okhttpTracesAndroid() {
        server.enqueue(MockResponse().setResponseCode(200))
        val span = SpanUtil.startSpan()
        span.makeCurrent().use {
            execute(span)
        }
        span.end()
        val spanItemsList: List<SpanData> = inMemorySpanExporter.finishedSpanItems
        assertThat(spanItemsList).hasSize(2)
        assertHttpHeader()

    }

    private fun assertHttpHeader() {
        val gson = Gson()
        val recordedRequest = server.takeRequest()
        val headers = recordedRequest.headers
        val json = gson.toJson(headers)
        println(json)
    }

    private fun execute(parentSpan: Span) {
        val client: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor(FixedTestInterceptor())
                .addInterceptor {
                    response(parentSpan, it)
                }
                .build()
        createCall(client).execute().close()
    }

    private fun response(parentSpan: Span, chain: Interceptor.Chain): Response {
        val currentSpan = Span.current().spanContext
        Assert.assertEquals(
                parentSpan.spanContext.traceId,
                currentSpan.traceId)
        return chain.proceed(chain.request())
    }

    private fun createCall(client: OkHttpClient): Call {
        val request: Request = Request.Builder().url(server.url("/test/")).build()
        return client.newCall(request)
    }


    @After
    @Throws(IOException::class)
    fun tearDown() {
        server.shutdown()
        inMemorySpanExporter.reset()
    }


    companion object {
        private val inMemorySpanExporter = InMemorySpanExporter.create()
    }
}
