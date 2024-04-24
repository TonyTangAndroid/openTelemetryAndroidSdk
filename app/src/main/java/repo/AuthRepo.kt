package repo

import app.AppContext
import app.DemoApp
import io.opentelemetry.context.Context
import io.reactivex.Single
import network.UserToken

class AuthRepo(private val app: AppContext) {

    fun auth(context: Context, success: Boolean): Single<UserToken> {
        return authInternal(if (success) 1 else 0, context)
    }

    private fun authInternal(flag: Int, context: Context): Single<UserToken> {
        return authWithExplicitOpenTelContext(flag, context)
    }

    private fun authWithExplicitOpenTelContext(flag: Int, context: Context): Single<UserToken> {
        return DemoApp.appScope(app).singleApi().logInWithContext(context, flag)
    }



}