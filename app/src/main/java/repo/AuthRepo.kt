package repo

import app.AppContext
import app.DemoApp
import app.OtelContextUtil
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.reactivex.Single
import network.UserToken

class AuthRepo(private val app: AppContext) {

    fun auth(success: Boolean): Single<UserToken> {
        return authInternal(if (success) 1 else 0)
    }

    private fun authInternal(flag: Int): Single<UserToken> {
        return authWithExplicitOpenTelContext(flag)
    }

    private fun authWithExplicitOpenTelContext(flag: Int): Single<UserToken> {
        val context: Context = explicitContext()
        return DemoApp.appScope(app).singleApi().logInWithContext(context, flag)
    }

    private fun explicitContext(): Context {
        val appScopeContext = OtelContextUtil.appScopeContext()
        return appScopeContext.with(attachedBaggage(appScopeContext))
    }

    private fun attachedBaggage(appScopeContext: Context): Baggage {
        return Baggage.fromContext(appScopeContext).toBuilder()
                .put("auth_token", "fixed_auth_token_value")
                .build()
    }


}