package app

import android.content.Context

data class ColdLaunchId(val uuid: String)
data class ColdLaunchModel(val coldLaunchId: ColdLaunchId, val timeMs : Long)
