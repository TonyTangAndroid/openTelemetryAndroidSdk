package io.opentelemetry.instrumentation.library.okhttp.v3_0

import io.opentelemetry.api.baggage.Baggage

object BaggageFactory {

    fun testBaggage(): Baggage {
        val baggageBuilder = Baggage.builder()
        baggageBuilder.put("abc", "456")
        return baggageBuilder.build()
    }

}