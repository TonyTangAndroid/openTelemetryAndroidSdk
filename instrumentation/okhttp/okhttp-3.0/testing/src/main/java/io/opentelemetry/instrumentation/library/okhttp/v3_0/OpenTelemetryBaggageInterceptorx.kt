package io.opentelemetry.instrumentation.library.okhttp.v3_0

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapSetter
import okhttp3.Interceptor
import okhttp3.Request.Builder
import okhttp3.Response
import java.io.IOException

class OpenTelemetryBaggageInterceptorx : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder: Builder = chain.request().newBuilder()
        val currentContext = Context.current().with(BaggageFactory.testBaggage())
        currentContext.makeCurrent().use {
            GlobalOpenTelemetry.getPropagators().textMapPropagator.inject(currentContext, requestBuilder, setter)
            return chain.proceed(requestBuilder.build())
        }
    }

    companion object {
        private val setter: TextMapSetter<Builder> = TextMapSetter<Builder>(::addHttpHeaderBaggage)
        private fun addHttpHeaderBaggage(carrier: Builder?, key: String, value: String) {
            carrier?.addHeader(key, value)
        }
    }
}