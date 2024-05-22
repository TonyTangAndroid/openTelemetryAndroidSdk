@file:Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")

package ui

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.wifi.WifiInfo
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import app.AppContext
import app.DemoApp
import app.WifiUtil
import com.example.hello_otel.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import network.CheckInResult
import network.CheckOutResult
import network.LocationEntity
import network.LocationModel
import network.LogOutStatus
import repo.CheckInRepo
import repo.CheckOutRepo
import repo.TokenStore
import timber.log.Timber
import java.util.UUID

class LoggedInFragment : Fragment() {
    private lateinit var authedContext: Context
    private lateinit var tvStatus: TextView
    private lateinit var tvDeviceModel: TextView
    private lateinit var tvWifiName: TextView
    private var progressDialogFragment: ProgressDialogFragment? = null
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }
    private val locationRequest by lazy {
        LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 3000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    fun setAuthedContext(authedContext: Context) {
        this.authedContext = authedContext
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_logged_in, container, false)
    }

    override fun onViewCreated(loggedInView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(loggedInView, savedInstanceState)
        tvStatus = loggedInView.findViewById(R.id.tv_status)
        tvDeviceModel = loggedInView.findViewById(R.id.tv_device_model)
        tvWifiName = loggedInView.findViewById(R.id.tv_wifi_name)
        loggedInView.findViewById<View>(R.id.btn_check_in).setOnClickListener {
            kickOffCheckIn(checkInContext("check_in_button_clicked"))
        }

        loggedInView.findViewById<View>(R.id.btn_check_out).setOnClickListener {
            checkingOut(checkoutWithBaggage(checkOutContext("check_out_button_clicked")))
        }
        loggedInView.findViewById<View>(R.id.btn_check_out_without_baggage).setOnClickListener {
            checkingOut(checkout())
        }
    }

    private fun checkInContext(interactionName: String): Context {
        return authedContext.with(Baggage.fromContext(authedContext).toBuilder()
                .put("interaction_uuid", UUID.randomUUID().toString())
                .put("interaction_name", interactionName)
                .build())
    }

    override fun onResume() {
        super.onResume()
        val deviceName = getDeviceName(requireContext())
        val deviceModel = android.os.Build.MODEL
        val wifiName =WifiUtil.wifi(AppContext(requireContext()))
        tvDeviceModel.text = "device_name: $deviceName\ndevice_model: $deviceModel"
        tvWifiName.text = "$wifiName"
    }


    private fun getDeviceName(context: android.content.Context): String? {
        return try {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        } catch (e: SecurityException) {
            // Handle the exception if the permission is not granted
            null
        }
    }

    private fun checkOutContext(interactionName: String): Context {
        return authedContext.with(Baggage.fromContext(authedContext).toBuilder()
                .put("interaction_uuid", UUID.randomUUID().toString())
                .put("interaction_name", interactionName)
                .build())
    }

    private fun kickOffCheckIn(context: Context) {
        val withCheckInStarted = context.with(attachedCheckInStarted(context))
        if (ActivityCompat.checkSelfPermission(requireActivity(), ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireActivity(), ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
            return
        }

        showProcessDialog()
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                onLocationResultReady(locationResult, withCheckInStarted)
            }
        }, Looper.getMainLooper())
    }


    private fun showProcessDialog() {
        progressDialogFragment = ProgressDialogFragment()
        progressDialogFragment?.show(childFragmentManager, ProgressDialogFragment.TAG)
    }

    private fun LocationCallback.onLocationResultReady(locationResult: LocationResult, context: Context) {
        fusedLocationClient.removeLocationUpdates(this)
        checkInWithLocation(locationResult, context.with(attachedLocationFetched(context)))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }

    private fun onPermissionDenied() {
        Toast.makeText(requireActivity(), "Permission Denied", Toast.LENGTH_SHORT).show()
    }

    private fun onPermissionGranted() {
        kickOffCheckIn(checkInContext("checked_button_clicked"))
    }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        setHasOptionsMenu(true)
    }

    private fun checkInWithLocation(location: LocationResult, context: Context) {
        Single.defer { checkingIn(locationResultModel(location), context) }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(AndroidLifecycleScopeProvider.from(this))
                .subscribe(this::updateStatus)
    }

    private fun locationResultModel(location: LocationResult): LocationModel {
        return LocationModel(location.locations.map { LocationEntity(it.latitude, it.longitude) })
    }

    private fun updateStatus(it: CheckInResult) {
        this.tvStatus.text = it.status
        progressDialogFragment?.dismiss()
    }

    private fun updateStatus(it: CheckOutResult) {
        this.tvStatus.text = it.status
        progressDialogFragment?.dismiss()
    }

    private fun checkingOut(call: Single<CheckOutResult>) {
        call
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(AndroidLifecycleScopeProvider.from(this))
                .subscribe(this::updateStatus)
    }

    private fun checkoutWithBaggage(context: Context): Single<CheckOutResult> {
        return CheckOutRepo(appContext()).withBaggage(context)
    }

    private fun checkout(): Single<CheckOutResult> {
        return CheckOutRepo(appContext()).withoutBaggage()
    }

    private fun checkingIn(locationModel: LocationModel, context: Context): Single<CheckInResult> {
        return CheckInRepo(appContext()).checkingIn(locationModel, context)
    }


    private fun loggingOut(): Single<LogOutStatus> {
        return DemoApp.appScope(appContext()).singleApi().logOut()
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
        loggingOutInternal()
        return true
    }

    private fun loggingOutInternal() {
        Single.defer { loggingOut() }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(AndroidLifecycleScopeProvider.from(this))
                .subscribe(this::onLogOutStatusReady)

    }

    private fun onLogOutStatusReady(status: LogOutStatus) {
        if (!status.loggedOut) {
            Toast.makeText(requireContext(), "Forcing logging out", Toast.LENGTH_SHORT).show()
        }
        TokenStore(appContext()).eraseToken()
        (requireActivity() as LoggedOutListener).onLoggedOut()
    }

    private fun appContext() = AppContext.from(requireContext())

    private fun attachedCheckInStarted(context: Context): Baggage {
        return Baggage.fromContext(context).toBuilder()
                .put("check_in_started", System.currentTimeMillis().toString())
                .build()
    }


    private fun attachedLocationFetched(context: Context): Baggage {
        return Baggage.fromContext(context).toBuilder()
                .put("location_fetched", System.currentTimeMillis().toString())
                .build()
    }


    interface LoggedOutListener {
        fun onLoggedOut()
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 0

    }
}

