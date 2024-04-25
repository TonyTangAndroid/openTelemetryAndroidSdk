package app

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context

data class OtelTracer(val tracer: Tracer)
data class OtelOpenTelemetry(val openTelemetry: OpenTelemetry)
data class OtelAuthActionModel(val authToken:String, val authActionContext: Context)
enum class EventType(val eventName: String) {
    APP_COLD_LAUNCH_STARTED("cold_launch_started"),
    APP_COLD_LAUNCH_ENDED("cold_launch_ended"),
    USER_CHECK_IN("check_in")
}