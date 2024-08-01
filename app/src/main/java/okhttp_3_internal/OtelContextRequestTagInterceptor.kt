package okhttp_3_internal

import io.opentelemetry.context.propagation.TextMapSetter
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal class OtelContextRequestTagInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val rawRequest = chain.request()
        return chain.proceed(rawRequest)
    }
}