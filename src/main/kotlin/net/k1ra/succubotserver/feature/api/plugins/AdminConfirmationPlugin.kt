package net.k1ra.succubotserver.feature.api.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import net.k1ra.succubotserver.feature.api.model.login.Session
import net.k1ra.succubotserver.feature.api.model.login.User

object AdminConfirmationPlugin {
    val adminConfirmationPlugin = createRouteScopedPlugin("AdminConfirmationPlugin") {
        onCall { call ->
            val session = Session.headerToSession(call.request.headers)

            if (User.isUserAdmin(session!!.uid))
                return@onCall

            //Otherwise, return error
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}