package app

import app.AppConstants.TAG_TEL
import timber.log.Timber

object TracingUtil {

     fun startSpan() {
        Timber.tag(TAG_TEL).i("Ignored OpenTelemetry Trace start action")
    }


    fun endSpan(interactiveSessionUuid: String) {
        Timber.tag(TAG_TEL).i("Ignored OpenTelemetry Trace end action:$interactiveSessionUuid")
    }


}

