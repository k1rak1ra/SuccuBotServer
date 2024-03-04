package net.k1ra.succubotserver.feature.api.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import net.k1ra.succubotserver.feature.api.model.login.Session

object AuthenticationPlugin {
    val authenticationPlugin = createRouteScopedPlugin("AuthenticationPlugin") {
        onCall { call ->
            val session = Session.headerToSession(call.request.headers)

            if (session != null && Session.isSessionValid(session))
                return@onCall

            //Otherwise, return error
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}