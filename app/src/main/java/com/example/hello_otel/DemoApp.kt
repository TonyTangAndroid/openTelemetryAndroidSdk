package com.example.hello_otel

import android.app.Application
import androidx.fragment.app.Fragment
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.extension.trace.propagation.JaegerPropagator
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import timber.log.Timber

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DemoApp : Application() {

    private val server = MockWebServer()

    override fun onCreate() {
        super.onCreate()
        initServer()
        plantTimberLogger()
        initOpenTelemetry()
    }

    private fun initServer() {
        server.start()
        repeat(10) {
            server.enqueue(MockResponse().setResponseCode(200))
        }
    }

    private fun initOpenTelemetry() {


        //step 1: config the telemetrySdk
        val inMemorySpanExporter = InMemorySpanExporter.create()
        val jaegerPropagator: JaegerPropagator = JaegerPropagator.getInstance()
        val spanProcessor: SpanProcessor = SimpleSpanProcessor.create(inMemorySpanExporter)
        val sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(spanProcessor)
                .build()
        val contextPropagators = ContextPropagators.create(jaegerPropagator)
        val telemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
                .setPropagators(contextPropagators)
                .build()
        GlobalOpenTelemetry.set(telemetrySdk)


        val tracer: Tracer = GlobalOpenTelemetry.getTracer("AppLaunchTracer")
        val spanBuilder: SpanBuilder = tracer.spanBuilder("A Test Span")
        val baggage = Baggage.builder()
                .put("user.name", "tonytang")
                .build()
        val makeCurrent: Scope = Context.current().with(baggage).makeCurrent()
        makeCurrent.use {
            spanBuilder.setAttribute("root_key_1", "root_key_2")
            spanBuilder.setSpanKind(SpanKind.CLIENT)
            val rootSpan = spanBuilder.startSpan()
            rootSpan.addEvent("started_event")

            //act
            rootSpan.makeCurrent()
            rootSpan.addEvent("ended_event")

        }

    }

    private fun plantTimberLogger() {
        Timber.plant(Timber.DebugTree())
        Timber.i("Demo App started")
    }
}