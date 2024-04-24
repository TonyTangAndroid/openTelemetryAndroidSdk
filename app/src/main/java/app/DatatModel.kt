package app

import android.content.Context

data class ColdLaunchId(val uuid: String)
data class ColdLaunchModel(val timeMs : Long, val coldLaunchId: ColdLaunchId)
