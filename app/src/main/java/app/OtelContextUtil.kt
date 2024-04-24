package app

import com.google.common.base.Suppliers
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.baggage.BaggageEntryMetadata
import io.opentelemetry.context.Context

object OtelContextUtil {

    private val cachedContext = Suppliers.memoize { rawContext() }

    private fun rawContext(): Context {
        val model = AppScopeUtil.coldLaunchModel()
        return Context.current().with(Baggage.builder()
                .put("cold_launch_uuid", model.coldLaunchId.uuid, BaggageEntryMetadata.create(model.timeMs.toString()))
                .put("cold_launch_uuid", model.timeMs.toString())
                .build())
    }

    fun cachedContext(): Context {
        return cachedContext.get()
    }

}

