package app

import com.google.common.base.Suppliers
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.baggage.BaggageEntryMetadata
import io.opentelemetry.context.Context
import java.util.UUID

object OtelContextUtil {

    private val cachedAppScopeContext = Suppliers.memoize { rawContext() }

    private fun rawContext(): Context {
        val model = AppScopeUtil.coldLaunchModel()
        return Context.current().with(Baggage.builder()
                .put("cold_launch_uuid", model.coldLaunchId.uuid, BaggageEntryMetadata.create(model.timeMs.toString()))
                .put("cold_launch_uuid_ms", model.timeMs.toString())
                .build())
    }

    fun appScopeContext(): Context {
        return cachedAppScopeContext.get()
    }

    fun authActionContext(): Context {
        val appScopeContext = appScopeContext()
        return appScopeContext.with(authActionBaggage(appScopeContext))
    }

    private fun authActionBaggage(appScopeContext: Context): Baggage {
        return Baggage.fromContext(appScopeContext).toBuilder()
                .put("auth_action_uuid", UUID.randomUUID().toString())
                .build()
    }

}

