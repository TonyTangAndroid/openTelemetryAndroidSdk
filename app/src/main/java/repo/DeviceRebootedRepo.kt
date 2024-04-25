package repo

import app.AppContext
import app.AppScopeUtil
import app.ColdLaunchModel
import app.DemoApp
import io.opentelemetry.context.Context
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import network.ColdLaunchData
import network.DeviceRebootedData
import network.DeviceRebootedResult

class DeviceRebootedRepo(private val appContext: AppContext) {

    fun notifyDeviceRebooted(rebootedContext: Context,action:String): Single<DeviceRebootedResult> {
        return Single.defer { notifyDeviceRebootedInternal(rebootedContext,action) }.subscribeOn(Schedulers.computation())
    }

    private fun notifyDeviceRebootedInternal(rebootedContext: Context,action:String): Single<DeviceRebootedResult> {
        val coldLaunchModel = AppScopeUtil.coldLaunchModel()
        val rebootTimeMs = System.currentTimeMillis()
        return DemoApp.appScope(appContext).singleApi().deviceRebooted(rebootedContext, deviceRebootedData(coldLaunchModel, rebootTimeMs, action))
    }

    private fun deviceRebootedData(coldLaunchModel: ColdLaunchModel, rebootTimeMs: Long, action: String): DeviceRebootedData {
        val data = ColdLaunchData(coldLaunchModel.coldLaunchId.uuid, coldLaunchModel.timeMs)
        return DeviceRebootedData(rebootTimeMs, action, data)
    }



}

