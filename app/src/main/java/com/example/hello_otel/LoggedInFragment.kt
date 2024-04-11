@file:Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")

package com.example.hello_otel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers

class LoggedInFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_logged_in, container, false)
    }

    override fun onViewCreated(loggedInView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(loggedInView, savedInstanceState)
        loggedInView.findViewById<View>(R.id.btn_check_in).setOnClickListener {
            checkIn(loggedInView.findViewById(R.id.tv_status))
        }

        loggedInView.findViewById<View>(R.id.btn_check_out).setOnClickListener {
            checkOut(loggedInView.findViewById(R.id.tv_status))
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setHasOptionsMenu(true)
    }

    private fun checkIn(tvStatus: TextView) {
        Single.defer { checkingIn() }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(AndroidLifecycleScopeProvider.from(this))
                .subscribe(
                        Consumer {
                            tvStatus.text = it.status
                        }
                )
    }

    private fun checkOut(tvStatus: TextView) {
        Single.defer { checkingOut() }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(AndroidLifecycleScopeProvider.from(this))
                .subscribe(
                        Consumer {
                            tvStatus.text = it.status
                        }
                )
    }

    private fun checkingIn(): Single<UserStatus> {
        return DemoApp.appScope(requireContext()).restApi().checkIn(TokenRepo(requireContext()).token())
    }

    private fun checkingOut(): Single<UserStatus> {
        return DemoApp.appScope(requireContext()).restApi().checkout()
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_logged_in, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> logOut()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logOut(): Boolean {
        eraseState()
        (requireActivity() as LoggedOutListener).onLoggedOut()
        return true
    }

    private fun eraseState() {
        TokenRepo(requireContext()).eraseToken()
    }

    interface LoggedOutListener {
        fun onLoggedOut()
    }
}