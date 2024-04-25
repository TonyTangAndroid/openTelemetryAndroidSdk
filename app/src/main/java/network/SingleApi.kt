package network

import io.opentelemetry.context.Context
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Tag

interface SingleApi {

    @POST("app_launch")
    fun appLaunch(@Tag context: Context, @Body coldLaunchData: ColdLaunchData, @Header("x-token") flag: String): Single<AppLaunchResult>

    @POST("device_rebooted")
    fun deviceRebooted(@Tag context: Context, @Body data: DeviceRebootedData): Single<DeviceRebootedResult>

    @POST("become_interactive")
    fun appBecomingInteractive(@Tag context: Context, @Body data: AppBecomeInteractiveData): Single<AppBecomeInteractiveResult>

    @GET("log_in")
    fun logInWithContext(@Tag context: Context, @Header("x-bypass") flag: Int): Single<UserToken>

    @GET("log_out")
    fun logOut(): Single<LogOutStatus>

    @POST("check_in")
    fun checkIn(@Tag context: Context? = null, @Body model: LocationModel, @Header("x-token") flag: String): Single<CheckInResult>

    @GET("check_out")
    fun checkoutWithoutBaggage(): Single<CheckOutResult>

    @GET("check_out")
    fun checkout(@Tag context: Context?): Single<CheckOutResult>


}