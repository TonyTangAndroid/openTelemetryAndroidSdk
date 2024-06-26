package app

import android.app.Application
import com.uber.autodispose.ScopeProvider
import com.uber.autodispose.autoDispose
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.data.SpanData
import network.AppLaunchResult
import network.MockWebServerUtil
import network.RestApiUtil
import network.SingleApi
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import repo.AppLaunchRepo
import timber.log.Timber
import java.util.concurrent.TimeUnit


class DemoApp : Application(), AppScope {
    private val server = MockWebServer()
    private val inMemorySpanExporter = InMemorySpanExporter.create()

    private val retrofit by lazy {
        RestApiUtil.retrofit(this, server)
    }

    private val singleApi by lazy {
        retrofit.create(SingleApi::class.java)
    }

    override fun onCreate() {
        super.onCreate()
        AppDelegatePreInitUtil.init()
        plantTimberLogger()
        OpenTelemetryUtil.configOpenTelemetry(inMemorySpanExporter)
        MockWebServerUtil.initServer(server)
        TracingUtil.startSpan()
        initHeavyOperation()
        Timber.tag(AppConstants.TAG_TEL).i("$this onCreate completed")

    }

    private fun initHeavyOperation() {
        AppLaunchRepo(appContext = AppContext(this)).notifyAppLaunch(
                OtelContextUtil.appScopeContext(), AppScopeUtil.coldLaunchModel()
        ).autoDispose(ScopeProvider.UNBOUND).subscribe(this::onAppLaunchResultFetched)
        delayColdLaunch()
    }

    /**
     * Purposefully use Sleep method to simulate the heavy initialization method.
     */
    private fun onAppLaunchResultFetched(appLaunchResponse: AppLaunchResult) {
        Timber.tag(LOG_TAG).i("app launch finished: $appLaunchResponse")
    }

    /**
     * Purposefully use Sleep method to simulate the heavy initialization method.
     */
    private fun delayColdLaunch() {
        Thread.sleep(500)
    }

    private fun plantTimberLogger() {
        Timber.plant(Timber.DebugTree())
        Timber.tag(LOG_TAG).i("Demo App started")
    }

    override fun dumpData(): List<SpanData> {
        return inMemorySpanExporter.finishedSpanItems
    }

    override fun singleApi(): SingleApi {
        return singleApi
    }

    override fun recordedRequest(): RecordedRequest? {
        return server.takeRequest(2, TimeUnit.SECONDS)
    }

    companion object {
        const val LOG_TAG = AppConstants.TAG_TEL
        fun appScope(context: AppContext): AppScope {
            return context.context.applicationContext as AppScope
        }
    }
}

