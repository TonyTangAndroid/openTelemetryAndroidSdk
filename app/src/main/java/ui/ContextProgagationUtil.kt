@file:Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")

package ui

import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context

object ContextProgagationUtil {

    fun attachedCheckInStarted(): Baggage {
        return Baggage.builder()
                .put("check_in_started", System.currentTimeMillis().toString())
                .build()
    }


    fun attachedLocationFetched(context: Context): Baggage {
        return Baggage.fromContext(context).toBuilder()
                .put("location_fetched", System.currentTimeMillis().toString())
                .build()
    }


    fun attachedSendingNetwork(context: Context): Baggage {
        return Baggage.fromContext(context).toBuilder()
                .put("sending_network", System.currentTimeMillis().toString())
                .build()
    }


}

