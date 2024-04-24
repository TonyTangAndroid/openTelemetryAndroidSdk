package app

import com.google.common.base.Suppliers
import io.opentelemetry.context.Context

object OtelContextUtil {

    private val cachedContext = Suppliers.memoize { rawContext() }

    private fun rawContext(): Context {
        return Context.current()
    }

    fun cachedContext(): Context {
        return cachedContext.get()
    }

}

