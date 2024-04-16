package network

import io.opentelemetry.context.Context
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Tag

interface SingleApi {

    @GET("log_in")
    fun logInWithContext(@Tag context: Context, @Header("x-bypass") flag: Int): Single<UserToken>

    @GET("log_in")
    fun logIn(@Header("x-bypass") flag: Int): Single<UserToken>

    @GET("log_out")
    fun logOut(): Single<LogOutStatus>

    @GET("check_in")
    fun checkIn(@Header("token") flag: String): Single<UserStatus>

    @GET("check_out")
    fun checkoutWithoutBaggage(): Single<UserStatus>

}