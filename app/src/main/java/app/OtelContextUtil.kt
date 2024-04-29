package app

import com.google.common.base.Suppliers
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.baggage.BaggageEntryMetadata
import io.opentelemetry.context.Context

object OtelContextUtil {

    private val cachedAppScopeContext = Suppliers.memoize { rootedContext() }

    private fun rootedContext(): Context {
        val model = AppScopeUtil.coldLaunchModel()
        return Context.current().with(Baggage.builder()
                .put("cold_launch_uuid", model.coldLaunchId.uuid, BaggageEntryMetadata.create(model.timeMs.toString()))
                .put("cold_launch_uuid_ms", model.timeMs.toString())
                .build())
    }

    fun appScopeContext(): Context {
        return cachedAppScopeContext.get()
    }

}

