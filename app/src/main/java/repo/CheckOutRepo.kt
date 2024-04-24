package repo

import app.AppContext
import app.DemoApp
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import network.CheckOutResult


class CheckOutRepo(private val appContext: AppContext) {

    fun withBaggage(context: Context): Single<CheckOutResult> {
        return Single.defer { withBaggageInternal(context) }
                .subscribeOn(Schedulers.computation())
    }

    private fun withBaggageInternal(context: Context): Single<CheckOutResult> {
        return DemoApp.appScope(appContext).singleApi().checkout(checkOutExtraContext(context))

    }

    private fun checkOutExtraContext(context: Context): Context {
        return context.with(Baggage.fromContext(context).toBuilder()
                .put("checkout_time_ms", System.currentTimeMillis().toString())
                .build())
    }

    fun withoutBaggage(): Single<CheckOutResult> {
        return Single.defer { withoutBaggageInternal() }.subscribeOn(Schedulers.computation())
    }

    private fun withoutBaggageInternal(): Single<CheckOutResult> {
        return DemoApp.appScope(appContext).singleApi().checkoutWithoutBaggage()

    }

}

