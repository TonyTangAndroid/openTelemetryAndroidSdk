package app

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.text.TextUtils
import android.widget.Toast
import timber.log.Timber

object WifiUtil {

    fun getCurrentNetworkDetail(context: AppContext) {
        val connManager =
            context.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        if (networkInfo?.isConnected == true) {
            val wifiManager = context.context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectionInfo = wifiManager.connectionInfo
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.ssid)) {
                logSsid(connectionInfo, context)
            } else {
                Timber.tag("SSID").e("SSID is empty or null")
            }
        } else {
            Timber.tag("SSID").e("No WiFi connection")
        }
    }

    private fun logSsid(info: WifiInfo, context: AppContext) {
        Timber.tag("SSID").e(info.ssid)
        Toast.makeText(context.context, "ssid:" + info.ssid, Toast.LENGTH_SHORT).show()
    }


}

