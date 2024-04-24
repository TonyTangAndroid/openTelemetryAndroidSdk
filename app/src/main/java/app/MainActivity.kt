package app

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.chuckerteam.chucker.api.Chucker
import com.example.hello_otel.R
import io.opentelemetry.context.Context
import repo.TokenStore
import timber.log.Timber
import ui.LoggedInFragment
import ui.LoggedOutFragment

class MainActivity : AppCompatActivity(), LoggedInFragment.LoggedOutListener, LoggedOutFragment.LoggedInListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Timber.tag(AppConstants.TAG_TEL).i("$this onCreate")
        TracingUtil.endSpan()
        if (TokenStore(AppContext.from(this)).isLoggedIn()) {
            bindLoggedInState()
        } else {
            bindLoggedOutState()

        }
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
        bindLoggedInState()
    }

    private fun bindLoggedInState() {
        bindFragment(LoggedInFragment())
    }

    private fun bindLoggedOutState() {
        bindFragment(LoggedOutFragment())
    }

    override fun onStart() {
        super.onStart()
        Timber.tag(AppConstants.TAG_TEL).i("$this onStart")

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
}