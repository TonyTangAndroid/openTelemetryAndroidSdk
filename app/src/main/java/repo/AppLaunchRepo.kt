package repo

import app.AppContext
import app.DemoApp
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.reactivex.Completable

class AppLaunchRepo(private val appContext: AppContext) {

    fun notifyAppLaunch(context: Context): Completable {
        val otelContext = context.with(attachedSendingNetwork(context))
        val token = TokenStore(appContext).token()
        return DemoApp.appScope(appContext).singleApi().appLaunch(otelContext,  token)
    }

    private fun attachedSendingNetwork(context: Context): Baggage {
        return Baggage.fromContext(context).toBuilder()
                .put("sending_network", System.currentTimeMillis().toString())
                .build()
    }


}

