package io.opentelemetry.instrumentation.library.okhttp.v3_0;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

class TestInjectingInterceptor implements Interceptor {
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request requestWithBaggage = getRequest(chain);
        return chain.proceed(requestWithBaggage);
    }

    @NonNull
    private static Request getRequest(@NonNull Chain chain) {
        return chain.request().newBuilder().addHeader("fixed_header_key", "fixed_header_value").build();
    }
}