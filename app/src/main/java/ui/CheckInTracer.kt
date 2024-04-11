package ui

import app.DemoApp
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.reactivex.Single
import network.UserStatus
import repo.TokenRepo


class CheckInTracer(private val context: android.content.Context) {
    fun checkIn() {
        val tracer: Tracer = GlobalOpenTelemetry.getTracer("CheckInTracer")
        Context.current().with(rootBaggage()).makeCurrent().use {
            triggerRootSpan(tracer )
        }
    }


    private fun triggerRootSpan(tracer: Tracer ): Span {
        val rootSpan: Span = rootSpan(tracer)
        rootSpan.addEvent("start_logging_in")
        //act
        rootSpan.makeCurrent().use {
            checkInInternal()
        }
        rootSpan.addEvent("finished_logging_in")
        rootSpan.end()
        return rootSpan
    }

    private fun checkInInternal(): UserStatus? {
        return DemoApp.appScope(context).restApi().checkout().execute().body()
    }


    private fun rootSpan(tracer: Tracer): Span {
        val spanBuilder: SpanBuilder = tracer.spanBuilder("A Test Span")
        spanBuilder.setAttribute("root_key_1", "root_key_2")
        spanBuilder.setSpanKind(SpanKind.CLIENT)
        return spanBuilder.startSpan()
    }


    /**
     * Configured the root baggage
     */
    private fun rootBaggage(): Baggage {
        return Baggage.builder()
                .put("user.name", "jack")
                .put("user.id", "321")
                .build()
    }

}

