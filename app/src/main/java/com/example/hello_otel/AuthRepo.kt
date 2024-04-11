package com.example.hello_otel

import android.content.Context
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class AuthRepo(private val app: Context) {

    private fun authInternal(flag: Int): Single<UserToken> {
        return Single.defer {
            DemoApp.appScope(app).restApi()
                    .login(flag)
        }
                .subscribeOn(Schedulers.io())
    }


    fun auth(success: Boolean): Single<UserToken> {
        return authInternal(if (success) 1 else 0)
    }


}