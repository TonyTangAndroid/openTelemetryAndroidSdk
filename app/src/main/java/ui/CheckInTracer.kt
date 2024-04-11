package ui

import app.DemoApp
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import network.UserStatus


class CheckInTracer(private val context: android.content.Context) {
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

