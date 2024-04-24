@file:Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")

package repo

import app.AppContext
import app.DemoApp
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.reactivex.Single
import network.CheckInResult
import network.LocationModel

class CheckInRepo(private val appContext: AppContext) {


    fun checkingIn(locationModel: LocationModel, context: Context): Single<CheckInResult> {
        val otelContext = context.with(attachedSendingNetwork(context))
        val token = TokenStore(appContext).token()
        return DemoApp.appScope(appContext).singleApi().checkIn(otelContext, locationModel, token)
    }

    private fun attachedSendingNetwork(context: Context): Baggage {
        return Baggage.fromContext(context).toBuilder()
                .put("sending_network", System.currentTimeMillis().toString())
                .build()
    }


}

