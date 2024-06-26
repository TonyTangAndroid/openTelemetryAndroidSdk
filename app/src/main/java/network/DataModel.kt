package network

import android.os.Bundle
import com.google.gson.annotations.SerializedName

data class UserToken(@SerializedName("token") val token: String)
data class ColdLaunchData(@SerializedName("cold_launch_uuid") val coldLaunchUuid: String, @SerializedName("time_ms") val timeMs: Long)
data class AppLaunchResult(@SerializedName("status") val status: String)
data class DeviceRebootedData(@SerializedName("device_rebooted_time_ms") val deviceRebootedTimeMs: Long,@SerializedName("action") val action:String, @SerializedName("cold_launch_data") val coldLaunchData: ColdLaunchData )
data class DeviceRebootedResult(@SerializedName("status") val status: String)
data class AppBecomeInteractiveData(@SerializedName("saved_interaction_session_uuid") val savedInteractionSessionUuid: String?, @SerializedName("cold_launch_data") val coldLaunchData: ColdLaunchData )
data class AppBecomeInteractiveResult(@SerializedName("status") val status: String)
data class CheckInResult(@SerializedName("status") val status: String)
data class CheckOutResult(@SerializedName("status") val status: String)
data class LogOutStatus(@SerializedName("logged_out") val loggedOut: Boolean)
data class LocationEntity(@SerializedName("lat") val lat: Double, @SerializedName("lng") val lng: Double)
data class LocationModel(@SerializedName("list") val list: List<LocationEntity>)
