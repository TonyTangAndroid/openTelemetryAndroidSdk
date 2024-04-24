package app

data class ColdLaunchId(val uuid: String)
data class ColdLaunchModel(val timeMs : Long, val coldLaunchId: ColdLaunchId)


