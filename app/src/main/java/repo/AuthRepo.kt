package repo

import app.AppContext
import app.DemoApp
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import network.UserToken

class AuthRepo(private val app: AppContext) {
    fun auth(success: Boolean): Single<UserToken> {
        return authInternal(if (success) 1 else 0)
    }

    private fun authInternal(flag: Int): Single<UserToken> {
        return Single.fromCallable {
            authWithBaggage(flag)
        }.subscribeOn(Schedulers.io())
    }

    private fun authWithBaggage(flag: Int): UserToken {
        return Context.current().with(attachedBaggage()).makeCurrent().use {
            userToken1(flag)
        }
    }

    private fun userToken1(flag: Int): UserToken {
        return DemoApp.appScope(app).singleApi().logIn(flag).blockingGet()
    }


    private fun attachedBaggage(): Baggage {
        return Baggage.builder()
                .put("cold_launch_id", "fixed_cold_launch_id")
                .build()
    }


}