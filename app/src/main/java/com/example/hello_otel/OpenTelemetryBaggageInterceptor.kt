package com.example.hello_otel

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapSetter
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.Response
import java.io.IOException

class OpenTelemetryBaggageInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()
        val requestBuilder: Builder = originalRequest.newBuilder()
        val baggageBuilder = Baggage.builder()
        baggageBuilder.put("abc", "456")
        val baggage = baggageBuilder.build()
        val currentContext = Context.current().with(baggage)
        currentContext.makeCurrent().use {
            GlobalOpenTelemetry.getPropagators().textMapPropagator.inject(currentContext, requestBuilder, setter)
            val newRequest: Request = requestBuilder.build()
            return chain.proceed(newRequest)
        }
    }

    companion object {
        private val setter: TextMapSetter<Builder> = TextMapSetter<Builder>(::extracted)
        private fun extracted(carrier: Builder?, key: String, value: String) {
            carrier?.addHeader(key, value)
        }
    }
}