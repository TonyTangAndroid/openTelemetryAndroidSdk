package com.example.hello_otel

import android.app.Application
import androidx.fragment.app.Fragment
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import timber.log.Timber

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DemoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        plantTimberLogger()
        initOpenTelmetry()
    }

    private fun initOpenTelmetry() {



        //step 1: config the telemetrySdk
        val inMemorySpanExporter = InMemorySpanExporter.create()
        val jaegerPropagator: JaegerPropagator = JaegerPropagator.getInstance()
        val spanProcessor: SpanProcessor = SimpleSpanProcessor.create(inMemorySpanExporter)
        val sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(spanProcessor)
                .build()
        //Make `uber-trace-id` attached.
        val contextPropagators = ContextPropagators.create(jaegerPropagator)
        val telemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
                .setPropagators(contextPropagators)
                .build()
        GlobalOpenTelemetry.set(telemetrySdk)

    }

    private fun plantTimberLogger() {
        Timber.plant(Timber.DebugTree())
        Timber.i("Demo App started")
    }
}