package repo

import android.os.Bundle
import app.AppContext
import app.AppScopeUtil
import app.DemoApp
import app.MainActivity
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import network.AppBecomeInteractiveData
import network.AppBecomeInteractiveResult
import network.ColdLaunchData

class ActivityCreatedRepo(private val appContext: AppContext) {

    fun notifyAppBecomingInteractive(context: Context, bundle: Bundle?): Single<AppBecomeInteractiveResult> {
        return Single.defer { notifyAppLaunchInternal(context, bundle) }.subscribeOn(Schedulers.computation())
    }

    private fun notifyAppLaunchInternal(context: Context, bundle: Bundle?): Single<AppBecomeInteractiveResult> {
        val otelContext = context.with(attachedStatus(context, bundle))
        return DemoApp.appScope(appContext).singleApi().appBecomingInteractive(otelContext, appBecomeInteractiveData(bundle))
    }

    private fun appBecomeInteractiveData(bundle: Bundle?): AppBecomeInteractiveData {
        val model = AppScopeUtil.coldLaunchModel()
        return AppBecomeInteractiveData(savedInteractiveSessionUuid(bundle), ColdLaunchData(model.coldLaunchId.uuid, model.timeMs))
    }

    private fun savedInteractiveSessionUuid(bundle: Bundle?): String? {
        return bundle?.getString(MainActivity.KEY_INTERACTIVE_SESSION_UUID)
    }

    private fun attachedStatus(context: Context, bundle: Bundle?): Baggage {
        val restored = savedInteractiveSessionUuid(bundle) != null
        return Baggage.fromContext(context).toBuilder().put("activity_restored", restored.toString()).build()
    }

}

