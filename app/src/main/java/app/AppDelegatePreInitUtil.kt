package app

import java.util.logging.Level
import java.util.logging.Logger

/**
 * Added to purposefully call the [AppScopeUtil] to initialize the cold launch model.
 * We do not want to delay the cold launch model initialization until it is actually used.
 */
object AppDelegatePreInitUtil {

    fun init() {
        val model = AppScopeUtil.coldLaunchModel()
        println("${AppConstants.TAG_TEL}:ColdLaunchModel accessed:$model")
        AndroidLoggingHandler.reset(AndroidLoggingHandler())
        Logger.getLogger(AppConstants.TAG_TEL).setLevel(Level.FINEST)
    }
}