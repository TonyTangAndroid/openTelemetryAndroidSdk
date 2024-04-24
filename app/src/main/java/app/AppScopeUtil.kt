package app

import com.google.common.base.Suppliers
import java.util.UUID

object AppScopeUtil {

    private val coldLaunchUuidSupplier = Suppliers.memoize { generateColdLaunchModel() }

    /**
     * A data model that is immutable across the cold launch.
     */
    private fun generateColdLaunchModel(): ColdLaunchModel {
        return ColdLaunchModel(System.currentTimeMillis(), ColdLaunchId(UUID.randomUUID().toString()))
    }

    fun coldLaunchModel(): ColdLaunchModel {
        return coldLaunchUuidSupplier.get()
    }
}