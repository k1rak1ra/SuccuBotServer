package net.k1ra.succubotserver.feature.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.k1ra.succubotserver.StorageManager
import net.k1ra.succubotserver.feature.api.plugins.AuthenticationPlugin
import java.io.File

fun Route.layout() {
    route("/layout/{did}") {
        install(AuthenticationPlugin.authenticationPlugin)
        get {
            val file = File(StorageManager.getMapStorageDir(call.parameters["did"]!!)+"/lay.bin")

            if (file.exists()) {
                call.respond(file.readBytes())
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}