package ui

import app.DemoApp
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.reactivex.Single
import network.UserStatus


class CheckOutRepo(private val context: android.content.Context) {


    fun checkingOutV2(): Single<UserStatus> {
        return Single.fromCallable { checkIn() }
    }


    fun checkIn(): UserStatus? {
        val scope = Context.current().with(rootBaggage()).makeCurrent()
        scope.use {
            return checkInInternal()
        }
    }


    private fun checkInInternal(): UserStatus? {
        return DemoApp.appScope(context).restApi().checkout().execute().body()
    }


    /**
     * Configured the root baggage
     */
    private fun rootBaggage(): Baggage {
        return Baggage.builder()
                .put("user.name", "jack")
                .put("user.id", "321")
                .build()
    }

}

