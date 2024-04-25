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
import io.opentelemetry.api.baggage.Baggage
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
        val interactiveContext = interactiveContext()
        trackActivityCreated(savedInstanceState, interactiveContext)
        val token = TokenStore(AppContext.from(this)).token()
        if (token.isNotEmpty()) {
            bindLoggedInState(authedContext(interactiveContext, token))
        } else {
            bindLoggedOutState()
        }
    }

    private fun authedContext(interactiveContext: Context, token: String): Context {
        return interactiveContext.with(Baggage.fromContext(interactiveContext).toBuilder()
                .put(KEY_AUTH_TOKEN, token)
                .build())
    }

    private fun initInteractiveSessionUuid(savedInstanceState: Bundle?) {
        interactiveSessionUuid = savedBundleId(savedInstanceState)
                ?: generateInteractiveSessionUuid()

    }

    private fun savedBundleId(savedInstanceState: Bundle?): String? {
        return savedInstanceState?.getString(KEY_INTERACTIVE_SESSION_UUID, null)
                .also {
                    Timber.tag(AppConstants.TAG_TEL).i("restored $KEY_INTERACTIVE_SESSION_UUID:$it")
                }
    }

    /**
     * Only end the span of a cold launch if it is absolute new.
     */
    private fun generateInteractiveSessionUuid(): String {
        return UUID.randomUUID().toString().also {
            Timber.tag(AppConstants.TAG_TEL).i("generateInteractiveSessionUuid:$it")
            TracingUtil.endSpan(it)
        }
    }

    private fun trackActivityCreated(savedInstanceState: Bundle?, interactiveContext: Context) {
        ActivityCreatedRepo(AppContext(this)).notifyAppBecomingInteractive(interactiveContext, savedInstanceState)
                .autoDispose(AndroidLifecycleScopeProvider.from(this))
                .subscribe(this::onResultReady)
    }

    private fun interactiveContext(): Context {
        return assembleInteractiveContext(OtelContextUtil.appScopeContext())
    }

    private fun assembleInteractiveContext(context: Context): Context {
        return context.with(Baggage.fromContext(context).toBuilder()
                .put(KEY_INTERACTIVE_SESSION_UUID, interactiveSessionUuid)
                .build())
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

    override fun onLoggedIn(actionModel: OtelAuthActionModel) {
        bindLoggedInState(toAuthedContext(actionModel))
    }

    private fun toAuthedContext(actionModel: OtelAuthActionModel): Context {
        val interactiveContext = assembleInteractiveContext(actionModel.authActionContext)
        return authedContext(interactiveContext, actionModel.authToken)
    }

    private fun bindLoggedInState(authedContext: Context) {
        val fragment = LoggedInFragment()
        fragment.setAuthedContext(authedContext)
        bindFragment(fragment)
    }

    private fun bindLoggedOutState() {
        bindFragment(LoggedOutFragment())
    }

    override fun onStart() {
        super.onStart()
        Timber.tag(AppConstants.TAG_TEL).i("$this onStart")

    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_INTERACTIVE_SESSION_UUID, interactiveSessionUuid)
        super.onSaveInstanceState(outState)
        Timber.tag(AppConstants.TAG_TEL).i("$this:saved $KEY_INTERACTIVE_SESSION_UUID:$interactiveSessionUuid")
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
        const val KEY_AUTH_TOKEN: String = "auth_token"
    }
}