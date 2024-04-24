package repo

import app.AppContext
import app.ColdLaunchModel
import app.DemoApp
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import network.AppLaunchResult
import network.ColdLaunchData

class AppLaunchRepo(private val appContext: AppContext) {

    fun notifyAppLaunch(context: Context, coldLaunchModel: ColdLaunchModel): Single<AppLaunchResult>  {
        return Single.defer { notifyAppLaunchInternal(context, coldLaunchModel) } .subscribeOn(Schedulers.computation())
    }

    private fun notifyAppLaunchInternal(context: Context, coldLaunchModel: ColdLaunchModel): Single<AppLaunchResult> {
        val otelContext = context.with(attachedSendingNetwork(context))
        val token = TokenStore(appContext).token()
        return DemoApp.appScope(appContext).singleApi().appLaunch(otelContext, coldLaunchData(coldLaunchModel), token)
    }

    private fun coldLaunchData(coldLaunchModel: ColdLaunchModel): ColdLaunchData {
        return ColdLaunchData(coldLaunchModel.coldLaunchId.uuid, coldLaunchModel.timeMs)
    }

    private fun attachedSendingNetwork(context: Context): Baggage {
        return Baggage.fromContext(context).toBuilder()
                .put("sending_network", System.currentTimeMillis().toString())
                .build()
    }


}

