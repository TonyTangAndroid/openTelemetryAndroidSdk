package model

sealed class Wifi
data class WifiName(val ssid: String) : Wifi()
object Absent : Wifi()
object Disconnected : Wifi()

