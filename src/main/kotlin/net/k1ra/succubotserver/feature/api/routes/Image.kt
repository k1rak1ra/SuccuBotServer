package net.k1ra.succubotserver.feature.api.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.k1ra.succubotserver.StorageManager
import java.io.File

fun Route.image() {
    route("/image/{id}") {
        get {
            val image = File(StorageManager.getUserImageFileWithName(call.parameters["id"]!!))

            call.respondBytes(image.readBytes())
        }
    }
}