package network

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
interface RestApi {
    @GET("log_in")
    fun login(@Header("x-bypass") flag: Int): Single<UserToken>

    @GET("log_out")
    fun logOut(): Single<LogOutStatus>

    @GET("check_in")
    fun checkIn(@Header("token") flag: String): Single<UserStatus>

    @GET("check_out")
    fun checkout(): retrofit2.Call<UserStatus>
}