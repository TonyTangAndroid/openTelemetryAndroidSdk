package xxx

import app.AppScopeUtil
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.baggage.BaggageEntryMetadata
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.extension.trace.propagation.JaegerPropagator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class UberTracingInterceptor ( private val contextPropagators: ContextPropagators) : Interceptor {


    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()
        val context = fixedContext()
        request = injectContextToRequest(request, context)
        return chain.proceed(request)
    }


    private fun fixedContext(): Context {
        val model = AppScopeUtil.coldLaunchModel()
        return Context.current().with(
            Baggage.builder()
                .put(
                    "cold_launch_uuidx",
                    model.coldLaunchId.uuid,
                    BaggageEntryMetadata.create(model.timeMs.toString())
                )
                .put("cold_launch_uuid_ms", model.timeMs.toString())
                .build()
        )
    }

    private fun injectContextToRequest(request: Request, context: Context): Request {
        val requestBuilder: Request.Builder = request.newBuilder()
        JaegerPropagator.getInstance()
            .inject(context, requestBuilder, RequestHeaderSetter.INSTANCE)
        return requestBuilder.build()
    }
}
