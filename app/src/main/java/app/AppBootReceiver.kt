package app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.uber.autodispose.ScopeProvider
import com.uber.autodispose.autoDispose
import io.opentelemetry.api.baggage.Baggage
import network.DeviceRebootedResult
import repo.DeviceRebootedRepo
import timber.log.Timber
import java.util.UUID

class AppBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.tag(AppConstants.TAG_TEL).i("$this:$context:$intent")
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Toast.makeText(context, "Greeting from HelloOtel", Toast.LENGTH_SHORT).show()
        }
        DeviceRebootedRepo(AppContext(context)).notifyDeviceRebooted(deviceRebootedContext(), intent.action ?: "invalid_intent_action")
                .autoDispose(ScopeProvider.UNBOUND)
                .subscribe(this::onResponseReceived)
    }

    private fun onResponseReceived(result: DeviceRebootedResult) {
        Timber.tag(AppConstants.TAG_TEL).i("onResponseReceived:$result")
    }

    private fun deviceRebootedContext(): io.opentelemetry.context.Context {
        val appScopeContext = OtelContextUtil.appScopeContext()
        return appScopeContext.with(Baggage.fromContext(appScopeContext).toBuilder()
                .put("device_rebooted_action_uuid", UUID.randomUUID().toString())
                .build())
    }
}
