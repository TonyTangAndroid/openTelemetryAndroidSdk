@file:Suppress("DEPRECATION")

package app

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.text.TextUtils
import model.Absent
import model.Disconnected
import model.Wifi
import model.WifiName

object WifiUtil {

    fun wifi(context: AppContext): Wifi {
        val manager = context.context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return if (networkInfo?.isConnected == true) {
            extractWifi(context)
        } else {
            Disconnected
        }
    }

    private fun extractWifi(context: AppContext): Wifi {
        val wifiManager = context.context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionInfo = wifiManager.connectionInfo
        return if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.ssid)) {
            WifiName(connectionInfo.ssid)
        } else {
            Absent
        }
    }


}

