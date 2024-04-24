package app

import io.opentelemetry.sdk.trace.data.SpanData
import network.SingleApi
import okhttp3.mockwebserver.RecordedRequest

interface AppScope {
    fun dumpData(): List<SpanData>
    fun singleApi(): SingleApi
    fun recordedRequest(): RecordedRequest?
}