package network

import io.opentelemetry.context.propagation.TextMapSetter
import okhttp3.Request.Builder
object RequestHeaderSetter : TextMapSetter<Builder> {
    override fun set(carrier: Builder?, key: String, value: String) {
        carrier?.header(key, value)
    }
}
