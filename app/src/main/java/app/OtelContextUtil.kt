package app

import app.AppConstants.TAG_TEL
import com.google.common.base.Suppliers
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import timber.log.Timber

object OtelContextUtil {

    private val coldLaunchSpan = Suppliers.memoize { startColdLaunchTracing() }

    private fun startColdLaunchTracing(): Span {
        val coldLaunchStartSpan = OpenTelemetryUtil.tracer().spanBuilder(EventType.APP_COLD_LAUNCH_STARTED.eventName).startSpan()
        coldLaunchStartSpan.makeCurrent().use {
            traceColdLaunchStartedSpan(coldLaunchStartSpan)
        }
        return coldLaunchStartSpan
    }

    /**
     * Purposefully explicitly manually add extra duplicated information, which could be already available in Otel framework.
     */
    private fun traceColdLaunchStartedSpan(coldLaunchStartSpan: Span) {
        val implicitTraceId = Span.current().spanContext.traceId
        val explicitTraceId = coldLaunchStartSpan.spanContext.traceId
        val explicitSpanId = coldLaunchStartSpan.spanContext.spanId
        Timber.tag(TAG_TEL).i("[start]:explicit_trace_id:$explicitTraceId,implicit_trace_id:$implicitTraceId")
        Timber.tag(TAG_TEL).i("[start]:start_explicit_span_id:$explicitSpanId")
        val coldLaunchUuid = AppScopeUtil.coldLaunchModel().coldLaunchId.uuid
        coldLaunchStartSpan.setAttribute("attr_cold_launch_id", coldLaunchUuid)
        val coldLaunchTimeMs = AppScopeUtil.coldLaunchModel().timeMs
        coldLaunchStartSpan.setAttribute("attr_launch_id_time_ms", coldLaunchTimeMs)
        val coldLaunchStartedEvent = coldLaunchStartedEvent(coldLaunchUuid, coldLaunchTimeMs)
        coldLaunchStartSpan.addEvent(EventType.APP_COLD_LAUNCH_STARTED.eventName, coldLaunchStartedEvent)
    }

    private fun coldLaunchStartedEvent(coldLaunchUuid: String, coldLaunchTimeMs: Long): Attributes {
        return Attributes.of(
                AttributeKey.stringKey("event_attr_cold_launch_id"), coldLaunchUuid,
                AttributeKey.longKey("event_attr_cold_launch_id_time_ms"), coldLaunchTimeMs)
    }

    fun startUpSpan(): Span {
        return coldLaunchSpan.get()
    }

    fun endSpan() {
        val startUpSpan: Span = startUpSpan()
        traceColdLaunchEndedSpan(startUpSpan)
        startUpSpan.end()

    }

    /**
     * Purposefully explicitly manually add extra duplicated information, which could be already available in Otel framework.
     */
    private fun traceColdLaunchEndedSpan(coldLaunchEndedSpan: Span) {
        val implicitTraceId = Span.current().spanContext.traceId
        val explicitTraceId = coldLaunchEndedSpan.spanContext.traceId
        val explicitSpanId = coldLaunchEndedSpan.spanContext.spanId
        Timber.tag(TAG_TEL).i("[end]:explicit_trace_id:$explicitTraceId,implicit_trace_id:$implicitTraceId")
        Timber.tag(TAG_TEL).i("[end]:explicit_span_id:$explicitSpanId")
        val coldLaunchUuid = AppScopeUtil.coldLaunchModel().coldLaunchId.uuid
        coldLaunchEndedSpan.setAttribute("attr_cold_launch_id", coldLaunchUuid)
        val coldLaunchTimeMs = AppScopeUtil.coldLaunchModel().timeMs
        coldLaunchEndedSpan.setAttribute("attr_launch_id_time_ms", coldLaunchTimeMs)
        val coldLaunchStartedEvent = coldLaunchStartedEvent(coldLaunchUuid, coldLaunchTimeMs)
        coldLaunchEndedSpan.addEvent(EventType.APP_COLD_LAUNCH_ENDED.eventName, coldLaunchStartedEvent)
        Timber.tag(TAG_TEL).i("[manual]:Cold launch span ended:$coldLaunchEndedSpan")
    }


}

