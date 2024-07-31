package network

import app.BundleTypeAdapterFactory
import app.DemoApp
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.propagation.ContextPropagators
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import xxx.UberTracingInterceptor

object RestApiUtil {

    fun retrofit(app: DemoApp, server: MockWebServer, contextPropagators1: ContextPropagators): Retrofit {

        val contextPropagators = contextPropagators1
        val client: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor(FirstFixedInterceptor())
                .addInterceptor(OtelContextRequestTagInterceptor())
                .addInterceptor(ChuckerInterceptor.Builder(app).createShortcut(true).build())
                .addInterceptor(UberTracingInterceptor(contextPropagators))
                .addInterceptor(SecondFixedInterceptor())
                .build()
        return Retrofit.Builder()
                .client(client)
                .baseUrl(server.url("rt/v1/"))
                .addConverterFactory(GsonConverterFactory.create(
                        gson()
                ))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build()
    }

    private fun gson(): Gson {
        return GsonBuilder()
                .registerTypeAdapterFactory(BundleTypeAdapterFactory()).create()
    }

}