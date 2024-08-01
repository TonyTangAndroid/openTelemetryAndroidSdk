/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package okhttp_3_internal

import app.OtelContextUtil
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
class OtelTracingInterceptor(
    private val instrumenter: Instrumenter<Request, Response>,
    private val propagators: ContextPropagators
) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var rawRequest: Request = chain.request()
        val parentContext = OtelContextUtil.appScopeContext()
        if (!instrumenter.shouldStart(parentContext, rawRequest)) {
            return chain.proceed(chain.request())
        }

        val context = instrumenter.start(parentContext, rawRequest)
        rawRequest = injectContextToRequest(rawRequest, context)

        var response: Response? = null
        var error: Throwable? = null
        try {
            context.makeCurrent().use {
                response = chain.proceed(rawRequest)
                return response!!
            }
        } catch (e: Exception) {
            error = e
            throw e
        } finally {
            instrumenter.end(context, rawRequest, response, error)
        }
    }


    // Context injection is being handled manually for a reason: we want to use the OkHttp Request
    // type for additional AttributeExtractors provided by the user of this library
    // thus we must use Instrumenter<Request, Response>, and Request is immutable
    private fun injectContextToRequest(request: Request, context: Context): Request {
        val requestBuilder: Request.Builder = request.newBuilder()
        propagators
            .textMapPropagator
            .inject(context, requestBuilder, RequestHeaderSetter.INSTANCE)
        return requestBuilder.build()
    }
}
