package interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

internal class FirstFixedInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request().newBuilder().addHeader("fixed_header_key_1", "1").build())
    }
}