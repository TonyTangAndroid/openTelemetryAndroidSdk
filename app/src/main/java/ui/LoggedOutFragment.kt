@file:Suppress("DEPRECATION")

package ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import app.AppContext
import app.OtelContextUtil
import com.example.hello_otel.R
import network.UserToken
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import repo.AuthRepo
import repo.TokenStore
import timber.log.Timber

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class LoggedOutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_logged_out, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.btn_login_success).setOnClickListener {
            auth(true)
        }
        view.findViewById<View>(R.id.btn_login_failure).setOnClickListener {
            auth(false)
        }
    }

    private fun auth(success: Boolean) {
        val authRepo = AuthRepo(appContext())
        val authContext = OtelContextUtil.authContext()
        authRepo.auth(authContext, success)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(AndroidLifecycleScopeProvider.from(this))
                .subscribe(
                        { onAuthSuccess(it,authContext) },
                        { onAuthError(it) }
                )

    }

    private fun onAuthSuccess(it: UserToken, authContext: io.opentelemetry.context.Context) {
        TokenStore(appContext()).saveToken(it.token)
        (requireActivity() as LoggedInListener).onLoggedIn()
    }

    private fun appContext() = AppContext.from(requireContext())

    private fun onAuthError(it: Throwable) {
        Timber.e(it)
        showLogInFailure()
    }


    private fun showLogInFailure() {
        Toast.makeText(requireContext(), "Username/Password wrong", Toast.LENGTH_SHORT).show()
    }


    interface LoggedInListener {
        fun onLoggedIn()
    }

}