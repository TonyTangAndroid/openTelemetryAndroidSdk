package repo

import app.AppContext
import app.DemoApp
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.reactivex.Single
import network.CheckInResult
import network.LocationModel
import timber.log.Timber

class CheckInRepo(private val appContext: AppContext) {


    fun checkingIn(locationModel: LocationModel, context: Context): Single<CheckInResult> {
        context.with(attachedSendingNetwork(context))
            .also {
                Timber.tag("otel_context").i("Ignored otel context in check-in $it")
            }
        val token = TokenStore(appContext).token()
        return DemoApp.appScope(appContext).singleApi().checkIn(locationModel, token)
    }

    private fun attachedSendingNetwork(context: Context): Baggage {
        return Baggage.fromContext(context).toBuilder()
            .put("sending_network", System.currentTimeMillis().toString())
            .build()
    }


}

