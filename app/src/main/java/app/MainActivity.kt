package app

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.chuckerteam.chucker.api.Chucker
import com.example.hello_otel.R
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import io.opentelemetry.context.Context
import network.AppBecomeInteractiveResult
import repo.ActivityCreatedRepo
import repo.TokenStore
import timber.log.Timber
import ui.LoggedInFragment
import ui.LoggedOutFragment
import java.util.UUID

class MainActivity : AppCompatActivity(), LoggedInFragment.LoggedOutListener, LoggedOutFragment.LoggedInListener {
    private lateinit var interactiveSessionUuid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initInteractiveSessionUuid(savedInstanceState)
        setContentView(R.layout.activity_main)
        Timber.tag(AppConstants.TAG_TEL).i("$this onCreate")
        TracingUtil.endSpan()
        trackActivityCreated(savedInstanceState)
        if (TokenStore(AppContext.from(this)).isLoggedIn()) {
            bindLoggedInState(activityScopeContext())
        } else {
            bindLoggedOutState()
        }
    }

    private fun initInteractiveSessionUuid(savedInstanceState: Bundle?) {
        interactiveSessionUuid = savedInstanceState?.getString(KEY_INTERACTIVE_SESSION_UUID, null)
                ?: generateInteractiveSessionUuid()

    }

    private fun generateInteractiveSessionUuid(): String {
        return UUID.randomUUID().toString()
    }

    private fun trackActivityCreated(savedInstanceState: Bundle?) {
        ActivityCreatedRepo(AppContext(this)).notifyAppBecomingInteractive(activityScopeContext(), savedInstanceState)
                .autoDispose(AndroidLifecycleScopeProvider.from(this))
                .subscribe(this::onResultReady)
    }

    private fun activityScopeContext(): Context {
        val appScopeContext = OtelContextUtil.appScopeContext()
        return appScopeContext
    }

    private fun onResultReady(result: AppBecomeInteractiveResult) {
        Timber.tag(AppConstants.TAG_TEL).i("AppBecomeInteractiveResult:$result")

    }

    private fun bindFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.root_fragment, fragment)
                .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_show_log -> showDialog()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDialog(): Boolean {
        startActivity(Chucker.getLaunchIntent(this))
        return true
    }

    override fun onLoggedOut() {
        bindLoggedOutState()

    }

    override fun onLoggedIn(authContext: Context) {
        bindLoggedInState(authContext)
    }

    private fun bindLoggedInState(authedContext: Context) {
        bindFragment(LoggedInFragment(authedContext))
    }

    private fun bindLoggedOutState() {
        bindFragment(LoggedOutFragment())
    }

    override fun onStart() {
        super.onStart()
        Timber.tag(AppConstants.TAG_TEL).i("$this onStart")

    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_INTERACTIVE_SESSION_UUID, interactiveSessionUuid);
        super.onSaveInstanceState(outState)
        Timber.tag(AppConstants.TAG_TEL).i("$this onSaveInstanceState saved $interactiveSessionUuid")
    }

    override fun onStop() {
        super.onStop()
        Timber.tag(AppConstants.TAG_TEL).i("$this onStop")

    }

    override fun onPause() {
        super.onPause()
        Timber.tag(AppConstants.TAG_TEL).i("$this onPause")
    }

    override fun onResume() {
        super.onResume()
        Timber.tag(AppConstants.TAG_TEL).i("$this onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(AppConstants.TAG_TEL).i("$this onDestroy")
    }

    companion object {
        const val KEY_INTERACTIVE_SESSION_UUID: String = "key_interactive_session_uuid"
    }
}