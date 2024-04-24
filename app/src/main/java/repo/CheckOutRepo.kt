package repo

import app.AppContext
import app.DemoApp
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import network.CheckInResult
import network.CheckOutResult


class CheckOutRepo(private val appContext: AppContext) {

    fun checkingOut(withBaggage: Boolean): Single<CheckOutResult> {
        Context.current().with(ignoredBaggage()).makeCurrent().use {
            return if (withBaggage) withBaggage() else withoutBaggage()
        }
    }

    private fun withBaggage(): Single<CheckOutResult> {
        return Single.fromCallable { withBaggageInternal() }
    }

    private fun withBaggageInternal(): CheckOutResult {
        Context.current().with(attachedBaggage()).makeCurrent().use {
            return DemoApp.appScope(appContext).singleApi().checkout().execute().body()!!
        }
    }

    private fun withoutBaggage(): Single<CheckOutResult> {
        return Single.defer { withoutBaggageInternal() }.subscribeOn(Schedulers.computation())
    }

    private fun withoutBaggageInternal(): Single<CheckOutResult> {
        return Context.current().with(attachedBaggageRx()).makeCurrent().use {
            DemoApp.appScope(appContext).singleApi().checkoutWithoutBaggage()
        }
    }

    private fun ignoredBaggage(): Baggage {
        return Baggage.builder()
                .put("user.name", "tony")
                .put("user.id", "321")
                .build()
    }

    private fun attachedBaggage(): Baggage {
        return Baggage.builder()
                .put("session_id", "test_session_id")
                .build()
    }

    private fun attachedBaggageRx(): Baggage {
        return Baggage.builder()
                .put("session_id", "ignored_session_id")
                .build()
    }

}

