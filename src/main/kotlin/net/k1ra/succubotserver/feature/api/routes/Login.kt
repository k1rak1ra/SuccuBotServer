package net.k1ra.succubotserver.feature.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.k1ra.succubotserver.feature.api.GsonProvider
import net.k1ra.succubotserver.feature.api.model.login.Ldap
import net.k1ra.succubotserver.feature.api.model.login.LoginRequest
import net.k1ra.succubotserver.feature.api.model.login.User
import net.k1ra.succubotserver.feature.api.model.settings.ServerSetting
import net.k1ra.succubotserver.feature.logging.Logger

fun Route.login() {
    route("/login") {
        post {
            val request = GsonProvider.gson.fromJson(call.receiveText(), LoginRequest::class.java)
            Logger.log("API", "Login request for ${request.email}")

            var user: User? = null

            //Try LDAP login first
            if (ServerSetting.getLdapLoginEnabled())
                user = Ldap().doLogin(request)

            //LDAP login didn't succeed try native
            if (user == null)
                user = User.login(request)

            if (user != null) {
                call.respondText(GsonProvider.gson.toJson(user).toString())
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}